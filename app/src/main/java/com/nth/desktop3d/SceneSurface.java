package com.nth.desktop3d;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** GPU scenery plus a lit 3D mesh. Native controls retain their accessibility. */
public final class SceneSurface extends GLSurfaceView implements GLSurfaceView.Renderer {
    private final String fragment;
    private final long start = SystemClock.uptimeMillis();
    private final FloatBuffer quad = buffer(new float[]{-1,-1,1,-1,-1,1,1,1});
    private final FloatBuffer crystal;
    private int program, meshProgram, width, height;
    private final float[] projection = new float[16], view = new float[16], model = new float[16], mvp = new float[16], temp = new float[16];
    private final float[] touchData = {0,0,-100, 0,0,-100, 0,0,-100, 0,0,-100};
    private int touchIndex;
    private volatile int sceneMode, count = 5;
    private volatile float tiltX, tiltY, strength = .5f, speed = .6f;
    private volatile boolean ambient = true, reduced, ripple = true, shadows = true;
    private int aPosition, uResolution, uTilt, uTime, uMotion, uMode, uCount, uSpeed, uRipple, uShadow, uTouches;
    private int mPosition, mNormal, mMatrix;
    public SceneSurface(Context context) {
        super(context);
        String source;
        try (java.io.InputStream in = context.getAssets().open("scene.frag")) {
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[4096];int read;
            while((read=in.read(bytes))!=-1)out.write(bytes,0,read);
            source=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (IOException e) { throw new IllegalStateException("Missing scene shader", e); }
        fragment = source;
        float[] points={0,1,0, -1,0,0, 0,0,1, 1,0,0, 0,0,-1, 0,-1,0};
        int[] faces={0,1,2,0,2,3,0,3,4,0,4,1,5,2,1,5,3,2,5,4,3,5,1,4};
        float[] data=new float[faces.length*6];
        for(int f=0;f<faces.length;f+=3) {
            int a=faces[f]*3,b=faces[f+1]*3,c=faces[f+2]*3;
            float ux=points[b]-points[a],uy=points[b+1]-points[a+1],uz=points[b+2]-points[a+2];
            float vx=points[c]-points[a],vy=points[c+1]-points[a+1],vz=points[c+2]-points[a+2];
            float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
            for(int j=0;j<3;j++) { int dst=(f+j)*6,src=faces[f+j]*3; data[dst]=points[src];data[dst+1]=points[src+1];data[dst+2]=points[src+2];data[dst+3]=nx;data[dst+4]=ny;data[dst+5]=nz; }
        }
        crystal=buffer(data);
        setEGLContextClientVersion(2); setEGLConfigChooser(8,8,8,0,16,0);
        setPreserveEGLContextOnPause(true); setRenderer(this); setRenderMode(RENDERMODE_WHEN_DIRTY);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    private static FloatBuffer buffer(float[] data) { FloatBuffer b=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(data).position(0);return b; }
    public void configure(LauncherStore store, boolean powerSave, boolean systemReduced) {
        sceneMode=store.mode; count=store.fishCount; strength=store.strength; speed=store.fishSpeed;
        reduced=store.reducedMotion || systemReduced; ambient=store.ambient && !powerSave && !reduced;
        ripple=store.ripples && !reduced; shadows=store.shadows;
    }
    public void tilt(float x,float y) { tiltX=x;tiltY=y; }
    public void ripple(float x,float y) { synchronized(touchData) { int i=(touchIndex++%4)*3;touchData[i]=x;touchData[i+1]=1-y;touchData[i+2]=(SystemClock.uptimeMillis()-start)/1000f; } requestRender(); }
    private static int shader(int type,String source) {
        int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,source);GLES20.glCompileShader(s);
        int[] ok=new int[1];GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0) throw new IllegalStateException("Scene shader: "+GLES20.glGetShaderInfoLog(s));return s;
    }
    private static int link(String vs,String fs) {
        int p=GLES20.glCreateProgram(),v=shader(GLES20.GL_VERTEX_SHADER,vs),f=shader(GLES20.GL_FRAGMENT_SHADER,fs);
        GLES20.glAttachShader(p,v);GLES20.glAttachShader(p,f);GLES20.glLinkProgram(p);
        int[] ok=new int[1];GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
        GLES20.glDeleteShader(v);GLES20.glDeleteShader(f);
        if(ok[0]==0) throw new IllegalStateException("Scene program: "+GLES20.glGetProgramInfoLog(p));return p;
    }
    @Override public void onSurfaceCreated(GL10 gl,EGLConfig config) {
        program=link("attribute vec2 position;void main(){gl_Position=vec4(position,0,1);}",fragment);
        aPosition=GLES20.glGetAttribLocation(program,"position");
        uResolution=GLES20.glGetUniformLocation(program,"resolution");uTilt=GLES20.glGetUniformLocation(program,"tilt");uTime=GLES20.glGetUniformLocation(program,"clockTime");
        uMotion=GLES20.glGetUniformLocation(program,"motion");uMode=GLES20.glGetUniformLocation(program,"mode");uCount=GLES20.glGetUniformLocation(program,"fishCount");
        uSpeed=GLES20.glGetUniformLocation(program,"fishSpeed");uRipple=GLES20.glGetUniformLocation(program,"rippleEnabled");uShadow=GLES20.glGetUniformLocation(program,"shadowEnabled");uTouches=GLES20.glGetUniformLocation(program,"touches");
        meshProgram=link("attribute vec3 position;attribute vec3 normal;uniform mat4 matrix;varying vec3 n;void main(){n=normal;gl_Position=matrix*vec4(position,1.0);}",
            "precision mediump float;varying vec3 n;void main(){float d=.35+.65*abs(dot(normalize(n),normalize(vec3(-.4,.8,1.0))));gl_FragColor=vec4(vec3(.28,.62,.95)*d,1.0);}");
        mPosition=GLES20.glGetAttribLocation(meshProgram,"position");mNormal=GLES20.glGetAttribLocation(meshProgram,"normal");mMatrix=GLES20.glGetUniformLocation(meshProgram,"matrix");
    }
    @Override public void onSurfaceChanged(GL10 gl,int w,int h) { width=w;height=h;GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,42,(float)w/h,.1f,100); }
    @Override public void onDrawFrame(GL10 gl) {
        if(width==0 || height==0)return;
        float t=(SystemClock.uptimeMillis()-start)/1000f;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glUseProgram(program);quad.position(0);GLES20.glEnableVertexAttribArray(aPosition);GLES20.glVertexAttribPointer(aPosition,2,GLES20.GL_FLOAT,false,0,quad);
        GLES20.glUniform2f(uResolution,width,height);GLES20.glUniform2f(uTilt,reduced?0:tiltX*strength,reduced?0:tiltY*strength);
        GLES20.glUniform1f(uTime,t);GLES20.glUniform1f(uMotion,ambient?1:0);GLES20.glUniform1f(uMode,sceneMode);GLES20.glUniform1f(uCount,count);
        GLES20.glUniform1f(uSpeed,speed);GLES20.glUniform1f(uRipple,ripple?1:0);GLES20.glUniform1f(uShadow,shadows?1:0);
        synchronized(touchData){GLES20.glUniform3fv(uTouches,4,touchData,0);}
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisableVertexAttribArray(aPosition);
        if(sceneMode<2) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);GLES20.glUseProgram(meshProgram);
            Matrix.setLookAtM(view,0,tiltX*strength*.14f,tiltY*strength*.14f,5,0,0,0,0,1,0);
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,.65f,.83f,0);
            Matrix.rotateM(model,0,ambient?t*7:22,0,1,0);Matrix.rotateM(model,0,20,1,0,0);Matrix.scaleM(model,0,.22f,.33f,.22f);
            Matrix.multiplyMM(temp,0,view,0,model,0);Matrix.multiplyMM(mvp,0,projection,0,temp,0);GLES20.glUniformMatrix4fv(mMatrix,1,false,mvp,0);
            crystal.position(0);GLES20.glEnableVertexAttribArray(mPosition);GLES20.glVertexAttribPointer(mPosition,3,GLES20.GL_FLOAT,false,24,crystal);
            crystal.position(3);GLES20.glEnableVertexAttribArray(mNormal);GLES20.glVertexAttribPointer(mNormal,3,GLES20.GL_FLOAT,false,24,crystal);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,24);GLES20.glDisableVertexAttribArray(mPosition);GLES20.glDisableVertexAttribArray(mNormal);
        }
    }
}
