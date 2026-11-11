precision highp float;
uniform vec2 resolution;
uniform vec2 tilt;
uniform float clockTime;
uniform float motion;
uniform float mode;
uniform float fishCount;
uniform float fishSpeed;
uniform float rippleEnabled;
uniform float shadowEnabled;
uniform vec3 touches[4];

float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
float noise(vec2 p) {
    vec2 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
float fbm(vec2 p) { return noise(p)*.55+noise(p*2.1)*.25+noise(p*4.3)*.13; }
float ellipse(vec2 p, vec2 scale) { return length(p/scale); }
mat2 rotate(float a) { return mat2(cos(a),-sin(a),sin(a),cos(a)); }

vec3 night(vec2 uv) {
    vec2 p=uv; p+=tilt*.018;
    vec3 col=mix(vec3(.035,.055,.14),vec3(.13,.18,.35),p.y);
    float glow=exp(-length((p-vec2(.77,.72))*vec2(1.1,1.5))*4.0);
    col+=vec3(.17,.15,.35)*glow;
    // Three independently displaced ridgelines.
    for(int i=0;i<3;i++) {
        float layer=float(i);
        float x=p.x+tilt.x*.018*layer;
        float h=.33-layer*.06+sin(x*6.0+layer*2.0)*.055+sin(x*13.0+layer)*.035;
        float mountain=1.0-smoothstep(h-.003,h+.003,p.y+tilt.y*.008*layer);
        col=mix(col,vec3(.055,.095,.18)-layer*.012,mountain*.86);
    }
    float floorMask=1.0-smoothstep(.18,.28,uv.y);
    vec2 floorUv=vec2((p.x-.5)/(p.y+.08),1.0/(p.y+.08))*3.0;
    vec2 g=abs(fract(floorUv)-.5);
    float grid=(1.0-smoothstep(.008,.025,min(g.x,g.y)))*floorMask;
    col+=vec3(.06,.12,.23)*grid*(.3+uv.y);
    return col;
}

vec3 hologram(vec2 uv) {
    vec2 p=uv+tilt*.025;
    vec3 col=vec3(.035,.028,.10);
    float n=fbm(p*4.0+vec2(clockTime*motion*.006,0));
    float band=exp(-pow((p.x-.52+sin(p.y*5.0)*.12)*3.0,2.0));
    col+=mix(vec3(.04,.11,.22),vec3(.25,.075,.31),n)*band*(.35+n);
    for(int layer=0;layer<3;layer++) {
        float d=float(layer)+1.0;
        vec2 s=p*vec2(35.0,55.0)/d+tilt*.12*d;
        vec2 cell=floor(s); vec2 pos=fract(s)-vec2(hash(cell),hash(cell+2.0));
        float star=(1.0-smoothstep(0.0,.045,length(pos)))*step(.94,hash(cell+5.0));
        col+=vec3(.55,.75,1.0)*star*(.35+.2*d);
    }
    vec2 pp=(uv-vec2(.78,.69)-tilt*.045)*vec2(resolution.x/resolution.y,1.0);
    float r=.092;
    float edge=1.0-smoothstep(r-.001,r+.002,length(pp));
    vec3 normal=normalize(vec3(pp/r,sqrt(max(0.0,1.0-dot(pp/r,pp/r)))));
    float lighting=max(.08,dot(normal,normalize(vec3(-.7,.7,.8))));
    col=mix(col,vec3(.18,.43,.68)*lighting,edge);
    col+=vec3(.09,.18,.36)*exp(-abs(length(pp)-r)*130.0)*.5;
    return col;
}

vec3 gravityScene(vec2 uv) {
    vec2 p=uv+tilt*.025;
    vec3 col=mix(vec3(.04,.035,.12),vec3(.17,.10,.30),p.y);
    col+=vec3(.13,.08,.22)*exp(-length(p-vec2(.5,.45))*4.0);
    float ring=abs(length((p-vec2(.5,.55))*vec2(1.0,.65))-.26);
    col+=vec3(.12,.10,.24)*(1.0-smoothstep(.001,.003,ring));
    float floorMask=1.0-smoothstep(.12,.4,p.y);
    col=mix(col,vec3(.055,.045,.13),floorMask*.6);
    return col;
}

vec3 pond(vec2 uv) {
    vec2 aspect=vec2(resolution.x/resolution.y,1.0);
    vec2 original=uv*aspect;
    vec2 p=original+tilt*.027;
    float t=clockTime*motion;
    vec2 distortion=vec2(sin(p.y*24.0+t*.55),cos(p.x*22.0+t*.42))*.0018*motion;
    float rippleLight=0.0;
    for(int i=0;i<4;i++) {
        float age=clockTime-touches[i].z;
        float distance=length(original-touches[i].xy*aspect);
        float envelope=exp(-distance*3.0)*exp(-age*1.3)*step(0.0,age)*step(age,3.5);
        float wave=sin(distance*105.0-age*8.0)*envelope*rippleEnabled;
        distortion+=normalize(original-touches[i].xy*aspect+vec2(.0001))*wave*.004;
        rippleLight+=wave*.025;
    }
    vec2 stoneP=(p+distortion)*13.0;
    vec2 cell=floor(stoneP), f=fract(stoneP);
    float d1=10.0,d2=10.0,identity=0.0; vec2 local=vec2(0);
    for(int x=-1;x<=1;x++) for(int y=-1;y<=1;y++) {
        vec2 c=cell+vec2(float(x),float(y));
        vec2 center=vec2(hash(c),hash(c+7.0))*.72+.14;
        vec2 delta=vec2(float(x),float(y))+center-f;
        float d=length(delta*vec2(.9,1.1));
        if(d<d1) { d2=d1;d1=d;identity=hash(c+22.0);local=delta; } else if(d<d2) {d2=d;}
    }
    float gap=smoothstep(.018,.07,d2-d1);
    vec3 stone=mix(vec3(.24,.32,.30),vec3(.57,.62,.51),identity);
    stone*=.63+max(0.0,1.0-length(local))*.35+local.y*.18-local.x*.12;
    stone+=noise(stoneP*20.0)*.018;
    vec3 col=mix(vec3(.07,.16,.15),stone,gap);
    // Fish occupy a middle layer, with offset shadows on the stone bed.
    for(int i=0;i<7;i++) {
        float fi=float(i);
        if(fi<fishCount) {
            float phase=fi*2.399+t*fishSpeed*.12;
            vec2 center=vec2(aspect.x*.5+sin(phase)*aspect.x*.39,.5+cos(phase*.83+fi)*.38);
            vec2 q=rotate(phase+.4)*(p-center+tilt*.012);
            float body=ellipse(q,vec2(.024,.061));
            float tail=ellipse(q-vec2(sin(t*3.0+fi)*.007,-.069),vec2(.025,.022));
            float fins=ellipse(q-vec2(0.0,-.007),vec2(.038,.014));
            float mask=(1.0-smoothstep(.88,1.03,min(body,min(tail,fins))))*.9;
            vec2 sq=rotate(phase+.4)*(p-center+vec2(.012,.019)+tilt*.008);
            float shadow=1.0-smoothstep(.75,1.35,ellipse(sq,vec2(.026,.065)));
            col*=1.0-shadow*.22*shadowEnabled;
            float patch=step(.53,noise(q*75.0+fi));
            vec3 fish=mix(vec3(.85,.81,.65),mix(vec3(.65,.16,.055),vec3(.12,.19,.17),step(.7,hash(vec2(fi)))),patch);
            fish*=.75+.25*(1.0-min(body,1.0));
            col=mix(col,fish,mask);
            float eye=1.0-smoothstep(.0015,.003,length(q-vec2(.010,.036)));
            col=mix(col,vec3(.035,.055,.05),eye);
        }
    }
    // Leaves sit above the fish and cast their own displaced shadows.
    for(int i=0;i<3;i++) {
        float fi=float(i);
        vec2 center=vec2(aspect.x*(.2+.30*fi)+sin(t*.07+fi)*.012,.25+.23*fi);
        vec2 q=rotate(fi*2.0+sin(t*.1)*.05)*(p-center+tilt*.025);
        float shape=ellipse(q,vec2(.034,.053));
        float leaf=(1.0-smoothstep(.94,1.01,shape));
        vec2 sq=q+vec2(.012,.015);
        col*=1.0-(1.0-smoothstep(.7,1.2,ellipse(sq,vec2(.035,.055))))*.19*shadowEnabled;
        float vein=1.0-smoothstep(.0005,.0012,abs(q.x));
        col=mix(col,mix(vec3(.25,.40,.10),vec3(.42,.57,.18),q.y/.11+.5)-vein*.06,leaf);
    }
    float caustic=pow(max(0.0,sin(p.x*37.0+sin(p.y*22.0+t*.4)*1.6)+cos(p.y*31.0-t*.25)),4.0)*.0015*motion;
    col+=vec3(.15,.25,.19)*(caustic+rippleLight);
    return col*.75+vec3(.015,.055,.06);
}

void main() {
    vec2 uv=gl_FragCoord.xy/resolution;
    vec3 col;
    if(mode<.5) col=night(uv);
    else if(mode<1.5) col=gravityScene(uv);
    else if(mode<2.5) col=hologram(uv);
    else col=pond(uv);
    // A gentle vignette keeps labels legible without flattening the scene.
    float vignette=1.0-.24*pow(length((uv-.5)*vec2(.8,1.0)),1.4);
    col*=vignette;
    col*=1.0-.20*smoothstep(.70,1.0,uv.y);
    gl_FragColor=vec4(col,1.0);
}
