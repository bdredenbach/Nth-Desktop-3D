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
uniform float waterLight;
uniform vec3 touches[4];
uniform sampler2D pondBed;
uniform sampler2D koiFish;
uniform sampler2D reefWater;
uniform sampler2D jellyfish;

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
    vec3 col=mix(vec3(.012,.030,.075),vec3(.065,.145,.26),p.y);
    // Crystal Glass atmosphere: independent luminous layers, not a static wallpaper.
    vec2 lens=(uv-vec2(.73,.68)-tilt*.028)*vec2(resolution.x/resolution.y,1.0);
    float halo=exp(-dot(lens,lens)*4.2);
    col+=vec3(.025,.17,.24)*halo;
    float beam=exp(-pow((lens.x+.20*lens.y)*5.5,2.0))*(.22+.78*smoothstep(.15,.95,uv.y));
    col+=vec3(.025,.09,.15)*beam;
    vec2 facets=(uv-vec2(.53,.54)-tilt*.012)*vec2(resolution.x/resolution.y,1.0);
    float diamond=abs(facets.x)*.75+abs(facets.y)*1.25;
    float rim=(1.0-smoothstep(.006,.022,abs(diamond-.34)))*.15;
    col+=vec3(.12,.55,.75)*rim*shadowEnabled;
    for(int i=0;i<3;i++){
        float layer=float(i)+1.0;
        vec2 stars=(uv+tilt*.009*layer)*vec2(23.0,37.0)/layer;
        vec2 cell=floor(stars), f=fract(stars)-.5;
        float sparkle=(1.0-smoothstep(.015,.08,length(f)))*step(.965,hash(cell+layer*9.0));
        col+=vec3(.32,.72,1.0)*sparkle*(.18+.16*layer);
    }
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
    col+=vec3(.035,.20,.27)*grid*(.3+uv.y);
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

// Sample straight-alpha photographs with the same vertical orientation on Android and EGL.
vec3 environment(sampler2D image,vec2 uv) {
    float viewAspect=resolution.x/resolution.y;
    vec2 crop=vec2(min(1.0,viewAspect/(2.0/3.0)),min(1.0,(2.0/3.0)/viewAspect));
    vec2 q=clamp((uv-.5)*crop*.92+.5,vec2(.002),vec2(.998));
    return texture2D(image,vec2(q.x,1.0-q.y)).rgb;
}
vec4 animal(sampler2D image,vec2 q) {
    if(q.x<.001||q.x>.999||q.y<.001||q.y>.999)return vec4(0.0);
    return texture2D(image,q);
}

// Refracted cell edges stretch and converge rather than sliding a flat light overlay.
float causticNet(vec2 p,float t) {
    p+=vec2(sin(p.y*1.25+t*.31),cos(p.x*1.12-t*.23))*.42;
    p+=vec2(t*.055,-t*.038);
    vec2 cell=floor(p),f=fract(p);float nearest=9.0,next=9.0;
    for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++){
        vec2 offset=vec2(float(x),float(y)),seed=cell+offset;
        vec2 phase=vec2(hash(seed),hash(seed+vec2(7.1,3.9)))*6.28318;
        vec2 point=.5+.30*sin(phase+vec2(t*.23,t*.19));
        vec2 delta=offset+point-f;float distance=dot(delta,delta);
        if(distance<nearest){next=nearest;nearest=distance;}else next=min(next,distance);
    }
    float ridge=1.0-smoothstep(.025,.14,next-nearest);
    float focus=.68+.32*sin(p.x*2.1+p.y*1.6+t*.37);
    return ridge*ridge*focus;
}
float stoneCaustics(sampler2D bed,vec2 uv,vec2 aspect,float t) {
    const vec3 luminance=vec3(.2126,.7152,.0722);
    // Image-derived relief anchors the light to the same rock contours as the photograph.
    // Interpolate a coarse relief field: photographic pebble grain must not become light noise.
    vec2 grid=vec2(14.0,21.0),cell=floor(uv*grid)/grid,f=fract(uv*grid),s=f*f*(3.0-2.0*f);
    float a=dot(environment(bed,cell),luminance),b=dot(environment(bed,cell+vec2(1.0/grid.x,0.0)),luminance);
    float c=dot(environment(bed,cell+vec2(0.0,1.0/grid.y)),luminance),d=dot(environment(bed,cell+1.0/grid),luminance);
    float height=mix(mix(a,b,s.x),mix(c,d,s.x),s.y);
    float dx=mix(b-a,d-c,s.y)*6.0*f.x*(1.0-f.x);
    float dy=mix(c-a,d-b,s.x)*6.0*f.y*(1.0-f.y);
    vec2 q=uv*aspect*18.0+vec2(dx,dy)*.65+height*.20;
    return causticNet(q,t);
}

vec3 pond(vec2 uv) {
    vec2 aspect=vec2(resolution.x/resolution.y,1.0),original=uv*aspect;
    vec2 p=original+tilt*.019;
    float t=clockTime*motion;
    vec2 distortion=vec2(sin(p.y*19.0+t*.43),cos(p.x*21.0+t*.34))*.0011*motion;
    float rippleLight=0.0;
    for(int i=0;i<4;i++) {
        float age=clockTime-touches[i].z,distance=length(original-touches[i].xy*aspect);
        float envelope=exp(-distance*3.0)*exp(-age*1.3)*step(0.0,age)*step(age,3.5);
        float wave=sin(distance*105.0-age*8.0)*envelope*rippleEnabled;
        distortion+=normalize(original-touches[i].xy*aspect+vec2(.0001))*wave*.004;
        rippleLight+=wave*.018;
    }
    vec2 bedUV=(p+distortion)/aspect;
    vec3 bed=environment(pondBed,bedUV);
    float light=stoneCaustics(pondBed,bedUV,aspect,t)*waterLight*(1.0-smoothstep(.91,1.0,uv.y));
    vec3 col=bed*(.82+light*.58)+vec3(.055,.09,.055)*light;
    // The fish follow independent smooth paths. Their forward direction is the path tangent.
    for(int i=0;i<7;i++) {
        float k=float(i);
        if(k<fishCount) {
            float phase=k*2.399+t*fishSpeed*(.10+k*.007);
            float radius=.27+.025*sin(k*1.7);
            vec2 center=vec2(aspect.x*(.5+sin(phase)*.31),.5+cos(phase*.83+k)*radius);
            vec2 direction=normalize(vec2(cos(phase)*aspect.x*.31,-sin(phase*.83+k)*radius*.83)+vec2(.00001));
            vec2 delta=p-center+distortion*.6+tilt*.009;
            vec2 q=vec2(dot(delta,vec2(direction.y,-direction.x)),dot(delta,direction));
            float lengthFish=.22+.015*sin(k*2.3),widthFish=lengthFish*(2.0/3.0);
            vec2 tex=vec2(q.x/widthFish+.5,.5-q.y/lengthFish);
            // Small head motion grows into a lateral tail stroke; fins retain their photographic detail.
            float tail=smoothstep(.35,.92,tex.y);
            tex.x+=sin(t*fishSpeed*4.1+k*1.9-tex.y*5.0)*.065*tail*tail*motion;
            float finWave=sin(t*fishSpeed*3.2+k)*.025*motion;
            tex.x=.5+(tex.x-.5)/(1.0+finWave*exp(-pow((tex.y-.30)*7.0,2.0)));
            vec4 fish=animal(koiFish,tex);
            vec2 sq=q+vec2(.009,.014);
            float shadow=(1.0-smoothstep(.65,1.5,ellipse(sq,vec2(widthFish*.20,lengthFish*.43))))*.18;
            col*=1.0-shadow*shadowEnabled;
            float colored=clamp((fish.r-fish.g)*2.5,0.0,1.0);
            vec3 marking=mod(k,3.0)<.5?vec3(.13,.15,.14):mod(k,3.0)>1.5?vec3(.94,.59,.15):vec3(.95,.24,.09);
            vec3 scales=mix(fish.rgb,marking*(.45+.55*fish.r),colored*.72);
            float light=.84+.12*sin(t*.23+k+q.y*14.0)*motion;
            col=mix(col,scales*vec3(.93,.98,.91)*light,fish.a*.96);
        }
    }
    col+=vec3(.025,.040,.030)*rippleLight;
    return col;
}

float roundedBox(vec2 p,vec2 size,float r) {
    vec2 q=abs(p)-size+r;
    return length(max(q,0.0))+min(max(q.x,q.y),0.0)-r;
}

// Rounded 3D optical solids. Rays enter a finite bevel rather than a neon outline.
vec3 atriumCenter(float k) {return vec3((k-1.5)*.435,-.065+sin(k*1.7)*.035,-.10*k);}
vec3 atriumLocal(vec3 v,float k) {v.xz=rotate(.30+k*.10)*v.xz;v.xy=rotate(-.08+k*.04)*v.xy;return v;}
vec3 atriumWorld(vec3 v,float k) {v.xy=rotate(.08-k*.04)*v.xy;v.xz=rotate(-.30-k*.10)*v.xz;return v;}
vec3 atriumSize(float k) {return vec3(.185,.77+k*.025,.105);}
float atriumDistance(vec3 p,vec3 b) {vec3 q=abs(p)-b+.047;return length(max(q,0.0))+min(max(q.x,max(q.y,q.z)),0.0)-.047;}
float atriumHit(vec3 ro,vec3 rd,float k,out vec3 point,out vec3 normal) {
    vec3 origin=atriumLocal(ro-atriumCenter(k),k),direction=atriumLocal(rd,k),b=atriumSize(k);
    vec3 inverse=mix(vec3(-1.0),vec3(1.0),step(vec3(0.0),direction))/max(abs(direction),vec3(.00001));
    vec3 a=(-b-origin)*inverse,c=(b-origin)*inverse;
    vec3 nearPlane=min(a,c),farPlane=max(a,c);
    float begin=max(0.0,max(nearPlane.x,max(nearPlane.y,nearPlane.z)));
    float end=min(farPlane.x,min(farPlane.y,farPlane.z));
    if(end<begin)return 100.0;
    float distance=begin;
    for(int stepIndex=0;stepIndex<28;stepIndex++) {
        point=origin+direction*distance;float surface=atriumDistance(point,b);
        if(surface<.00035){vec3 edge=max(abs(point)-b+.047,0.0);normal=normalize(edge*sign(point)+vec3(.000001));return distance;}
        distance+=max(surface,.00015);if(distance>end)break;
    }
    return 100.0;
}
vec3 atriumEnvironment(vec3 direction,float t) {
    vec3 col=mix(vec3(.012,.024,.040),vec3(.10,.17,.23),smoothstep(-.5,.9,direction.y));
    vec3 key=normalize(vec3(-.85+sin(t*.23)*.22,.10,1.0));
    float softbox=pow(max(0.0,dot(direction,key)),32.0);
    float strip=exp(-pow((direction.x+.38*direction.z-sin(t*.19)*.12)*45.0,2.0))*smoothstep(-.65,.1,direction.y);
    float rim=pow(max(0.0,dot(direction,normalize(vec3(.9,.15,-.65)))),65.0);
    float panel=exp(-pow((direction.x+.80*direction.z-sin(t*.19)*.10)*46.0,2.0));
    panel+=.50*exp(-pow((direction.x+1.25*direction.z+sin(t*.17)*.08)*55.0,2.0));
    panel*=1.0-smoothstep(.58,.88,abs(direction.y));
    col+=vec3(1.6,2.1,2.6)*softbox+vec3(7.0,8.2,8.8)*panel+vec3(5.0,6.3,7.0)*strip+vec3(.40,1.3,2.1)*rim;
    return col;
}
vec3 atriumBackdrop(vec3 ro,vec3 rd,float t) {
    vec3 col=atriumEnvironment(rd,t)*.075;
    if(rd.y<-.0001) {
        float distance=(-1.02-ro.y)/rd.y;
        if(distance>0.0) {
            vec3 hit=ro+rd*distance;vec2 floorPoint=hit.xz;
            float grain=fbm(floorPoint*3.0);
            vec3 floorColor=vec3(.004,.008,.013)+vec3(.006,.009,.012)*grain;
            float seam=min(abs(fract(floorPoint.x*.65+.5)-.5),abs(fract(floorPoint.y*.65+.5)-.5));
            floorColor*=.78+.22*smoothstep(.003,.012,seam);
            float light=0.0,contact=0.0;
            for(int j=0;j<4;j++) {
                float k=float(j);vec2 offset=floorPoint-atriumCenter(k).xz;
                contact+=exp(-dot(offset*vec2(3.7,2.2),offset*vec2(3.7,2.2)))*.32;
                vec2 focused=offset-vec2(.10+sin(t*.23+k)*.08,.14+cos(t*.19+k)*.10);
                float pool=exp(-dot(focused*vec2(3.0,1.4),focused*vec2(3.0,1.4)));
                float fold=focused.x*10.0+sin(focused.y*9.0+t*.45+k)*.55+sin(t*.31+k)*.45;
                light+=pool*(.10+.46*exp(-pow(fold*2.3,2.0))+.14*pow(.5+.5*sin(focused.y*17.0-t*.5+k),9.0));
            }
            floorColor*=1.0-min(.70,contact)*shadowEnabled;
            floorColor+=vec3(.11,.34,.46)*light;
            float fresnel=.035+.45*pow(1.0-max(0.0,-rd.y),5.0);
            floorColor+=atriumEnvironment(reflect(rd,vec3(0,1,0)),t)*fresnel*.065;
            col=mix(col,floorColor,exp(-distance*.028));
        }
    }
    return col;
}
vec3 atrium(vec2 uv) {
    vec2 p=(uv-.5)*vec2(resolution.x/resolution.y,1.0);
    float t=clockTime*motion;
    vec3 ro=vec3(tilt.x*.13,.22+tilt.y*.09,4.5);
    vec3 rd=normalize(vec3(p*3.9-tilt*.055+vec2(0,-.30),-4.5));
    vec3 col=atriumBackdrop(ro,rd,t),point=vec3(0),normal=vec3(0);
    float nearest=100.0,chosen=0.0;vec3 localPoint=vec3(0),localNormal=vec3(0);
    for(int i=0;i<4;i++) {
        float k=float(i);float distance=atriumHit(ro,rd,k,point,normal);
        if(distance<nearest){nearest=distance;chosen=k;localPoint=point;localNormal=normal;}
    }
    if(nearest<99.0) {
        vec3 hit=ro+rd*nearest,n=atriumWorld(localNormal,chosen);
        float facing=max(.001,dot(-rd,n));
        float fresnel=.042+.958*pow(1.0-facing,5.0);
        vec3 inside=refract(rd,n,1.0/1.52);
        float thickness=clamp(.21/max(.18,abs(inside.z)),.20,.85);
        vec3 exitPoint=hit+inside*thickness;
        // Separate color paths give a restrained dispersion at the curved bevel.
        vec3 bend=n*(.018+.035*(1.0-facing));
        vec3 transmission;
        transmission.r=atriumBackdrop(exitPoint+bend*1.035,rd,t).r;
        transmission.g=atriumBackdrop(exitPoint+bend,rd,t).g;
        transmission.b=atriumBackdrop(exitPoint+bend*.965,rd,t).b;
        transmission*=exp(-vec3(.23,.075,.045)*thickness);
        vec3 reflected=atriumEnvironment(reflect(rd,n),t);
        float backGlint=pow(max(0.0,dot(reflect(inside,-n),normalize(vec3(-.7,.8,1.0)))),55.0);
        col=transmission*(1.0-fresnel)+reflected*fresnel+vec3(.35,.65,.80)*backGlint*.14;
        // A clear front face carries a broad reflected studio light, not a fill tint.
        col+=reflected*.018*(1.0-fresnel);
    }
    return pow(vec3(1.0)-exp(-max(col,0.0)*1.25),vec3(.4545));
}

vec3 aurora(vec2 uv) {
    vec2 p=uv+tilt*.024;
    float t=clockTime*motion*.075,horizon=.27;
    vec2 sky=vec2(p.x,horizon+abs(p.y-horizon));
    vec3 col=mix(vec3(.018,.055,.105),vec3(.008,.017,.05),smoothstep(horizon,1.0,sky.y));
    for(int i=0;i<3;i++) {
        float k=float(i);
        float curtain=.47+k*.105+sin(sky.x*5.0+t+k*1.8)*.07+sin(sky.x*13.0-t*.5+k)*.025;
        float delta=sky.y-curtain;
        float ribbon=exp(-max(0.0,delta)*12.0)*smoothstep(-.018,.012,delta);
        ribbon*=.45+.55*noise(vec2(sky.x*38.0+t,sky.y*.8+k));
        col+=mix(vec3(.025,.48,.38),vec3(.25,.10,.47),k*.4)*ribbon*.72;
    }
    vec2 stars=(sky+tilt*.012)*vec2(75,120),cell=floor(stars),f=fract(stars)-.5;
    col+=vec3(.4,.55,.7)*(1.0-smoothstep(.015,.085,length(f)))*step(.979,hash(cell));
    float mountain=horizon+.035+sin(p.x*16.0)*.022+sin(p.x*31.0+1.0)*.013;
    float ridge=1.0-smoothstep(mountain-.002,mountain+.002,sky.y);
    col=mix(col,vec3(.012,.028,.047),ridge);
    if(p.y<horizon){
        float shimmer=sin(p.y*185.0+t*2.0+sin(p.x*17.0)*1.5)*.5+.5;
        col*=.40+.13*shimmer;col+=vec3(.018,.06,.075)*exp(-abs(p.y-horizon)*8.0);
    }
    return col;
}

vec3 reef(vec2 uv) {
    vec2 aspect=vec2(resolution.x/resolution.y,1.0),p=uv*aspect+tilt*.017;
    float t=clockTime*motion;
    vec2 flow=vec2(sin(p.y*6.0+t*.12),cos(p.x*9.0+t*.09))*.0008*motion;
    vec2 bedUV=(p+flow)/aspect;vec3 bed=environment(reefWater,bedUV);
    float rock=1.0-smoothstep(.10,.38,uv.y);
    rock*=mix(.18,1.0,smoothstep(.18,.53,bed.r/max(.025,bed.g)));
    float caustic=stoneCaustics(reefWater,bedUV,aspect,t*.82)*rock*waterLight;
    vec3 col=bed*(.70+caustic*.85)+vec3(.025,.10,.12)*caustic;
    // Broad shafts breathe and slowly change angle from the moving water surface.
    float rays=0.0;
    for(int beam=0;beam<3;beam++){
        float k=float(beam),source=.04+k*.28+sin(t*.33+k*1.7)*.070;
        float slope=.29+sin(t*.23+k)*.075;
        float distance=uv.x-source-(1.0-uv.y)*slope;
        float width=.033+(1.0-uv.y)*.028;
        rays+=exp(-pow(distance/width,2.0))*(.62+.38*sin(t*.72+k*2.0))*smoothstep(.03,.85,uv.y);
    }
    float dance=(sin(uv.x*13.0+uv.y*5.0-t*.85)+sin(uv.x*24.0-uv.y*8.0+t*.57))*.5;
    float leftShaft=exp(-pow((uv.x-.20)/.40,2.0))*smoothstep(.15,.92,uv.y);
    col*=1.0+dance*leftShaft*.16*waterLight;
    float surface=smoothstep(.80,.99,uv.y)*(.5+.5*sin(uv.x*29.0+sin(uv.x*11.0+t*.31)*2.0-t*.43));
    col+=vec3(.025,.09,.12)*(rays*.65+surface*.25)*waterLight;
    // Each animal has its own phase, depth and speed; no tilt is needed for swimming.
    for(int i=0;i<4;i++) {
        float k=float(i),depth=.72+k*.085;
        vec2 center=vec2(aspect.x*(.17+k*.22+sin(t*(.075+k*.008)+k)*.055),.24+k*.17+sin(t*(.15-k*.012)+k*1.9)*.038);
        vec2 q=rotate(sin(t*.12+k*2.0)*.075)*(p-center-tilt*k*.004);
        float height=.25+k*.037,width=height*(2.0/3.0);
        vec2 tex=vec2(q.x/width+.5,.5-q.y/height);
        float phase=t*(1.15+k*.075)+k*2.1,pulse=sin(phase);
        float arms=smoothstep(.23,.88,tex.y);
        tex.x=.5+(tex.x-.5)/(1.0+.08*pulse*(1.0-arms)*motion);
        tex.y=.22+(tex.y-.22)*(1.0+.05*pulse*(1.0-arms)*motion);
        tex.x+=sin(tex.y*14.0-t*(.8+k*.05)+k)*.022*arms*motion;
        vec4 jelly=animal(jellyfish,tex);
        vec3 tint=mix(vec3(.74,.93,1.0),vec3(.89,.81,1.0),k/3.0);
        float shimmer=1.0+sin(t*.42+k*1.3+tex.y*2.0)*.055*waterLight;
        col=mix(col,jelly.rgb*tint*depth*shimmer,jelly.a*(.68+k*.045));
    }
    vec2 dust=(p+vec2(0,-t*.0013))*vec2(55,80),cell=floor(dust),f=fract(dust)-.5;
    col+=vec3(.018,.05,.055)*(1.0-smoothstep(.02,.10,length(f)))*step(.97,hash(cell));
    return col;
}

void main() {
    vec2 uv=gl_FragCoord.xy/resolution;
    vec3 col;
    if(mode<.5) col=night(uv);
    else if(mode<1.5) col=gravityScene(uv);
    else if(mode<2.5) col=hologram(uv);
    else if(mode<3.5) col=pond(uv);
    else if(mode<4.5) col=atrium(uv);
    else if(mode<5.5) col=aurora(uv);
    else col=reef(uv);
    // A gentle vignette keeps labels legible without flattening the scene.
    float vignette=1.0-.24*pow(length((uv-.5)*vec2(.8,1.0)),1.4);
    col*=vignette;
    col*=1.0-.20*smoothstep(.70,1.0,uv.y);
    gl_FragColor=vec4(col,1.0);
}
