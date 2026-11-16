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
uniform sampler2D glassSky;
uniform sampler2D focusCity;
uniform sampler2D iceMaterial;
uniform sampler2D lavaMaterial;

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

// Photographs retain detail while live geometry owns depth and material movement.
vec2 photoCover(vec2 uv,float sourceAspect,float overscan) {
    float aspect=resolution.x/resolution.y;
    vec2 crop=vec2(min(1.0,aspect/sourceAspect),min(1.0,sourceAspect/aspect));
    return clamp(.5+(uv-.5)*crop*overscan,vec2(.003),vec2(.997));
}
vec3 skyPhoto(vec2 uv) {return texture2D(glassSky,vec2(clamp(uv.x,.003,.997),1.0-clamp(uv.y,.003,.997))).rgb;}
vec3 paneLocal(vec3 v,float yaw,float pitch,float roll) {v.xz=rotate(yaw)*v.xz;v.yz=rotate(pitch)*v.yz;v.xy=rotate(roll)*v.xy;return v;}
vec3 atrium(vec2 uv) {
    float t=clockTime*motion,aspect=resolution.x/resolution.y;
    vec2 p=(uv-.5)*vec2(aspect,1.0),background=photoCover(uv,1.0,.86)+tilt*.018;
    vec3 col=skyPhoto(background)*vec3(.80,.89,.97);
    vec3 ro=vec3(tilt*.10,3.0),rd=normalize(vec3(p*2.8,-3.0));
    for(int i=0;i<9;i++) {
        float k=float(i),column=mod(k,3.0)-1.0,row=floor(k/3.0)-1.0,depth=.25+k*.095;
        vec3 center=vec3(column*aspect*1.15,row*1.08,-.50+k*.105);
        center.xy+=vec2(sin(t*.19+k*1.7),cos(t*.16+k*2.1))*.035+tilt*depth*.10;
        float yaw=.26*sin(k*1.7)+tilt.x*.15+sin(t*.11+k)*.035;
        float pitch=.15*cos(k*1.2)+tilt.y*.12,roll=.12*sin(k*2.4);
        vec3 origin=paneLocal(ro-center,yaw,pitch,roll),direction=paneLocal(rd,yaw,pitch,roll);
        float distance=-origin.z/min(-.001,direction.z);vec2 q=(origin+direction*distance).xy;
        vec2 size=vec2(aspect*.70,.72);float sd=roundedBox(q,size,.007);
        float aa=2.5/max(540.0,resolution.y),mask=1.0-smoothstep(-aa,aa,sd);
        if(mask>.001) {
            vec2 bend=vec2(yaw,pitch)*.060+q*.012;
            vec3 transmitted=skyPhoto(background+bend+tilt*depth*.015)*vec3(.71,.88,.97);
            vec2 mirror=clamp(vec2(.50)+q/vec2(aspect*2.9,2.9)+vec2(k*.045-.16,.12*sin(k))+tilt*.09+vec2(sin(t*.13+k)*.012,0),.015,.985);
            vec3 reflection=skyPhoto(mirror);float fresnel=.10+.40*pow(abs(sin(yaw+.25*rd.x)),2.0);
            vec3 glass=mix(transmitted,reflection*vec3(.75,.89,1.02),fresnel);
            float reflectedLight=exp(-pow((q.x+.42*q.y+sin(t*.15+k)*.16)*4.0,2.0));
            glass+=vec3(.035,.065,.082)*reflectedLight;
            float frame=1.0-smoothstep(.006,.015,abs(sd+.008));
            float side=1.0-smoothstep(.008,.019,abs(roundedBox(q+vec2(.012,-.012),size,.007)));
            glass=mix(glass,vec3(.016,.027,.035),max(frame,side*.70));
            float glint=(1.0-smoothstep(.001,.004,abs(sd+.016)))*(.22+.78*pow(.5+.5*sin(q.y*2.0+t*.26+k),3.0));
            glass+=vec3(.30,.43,.49)*glint;col=mix(col,glass,mask*.94);
        }
    }
    return col;
}
// The two halves are registered copies: sharp on the left, defocused on the right.
vec3 cityPhoto(vec2 uv,float blurred) {vec2 q=clamp(uv,vec2(.004),vec2(.996));return texture2D(focusCity,vec2(q.x*.5+blurred*.5,1.0-q.y)).rgb;}
float segmentDistance(vec2 p,vec2 a,vec2 b){vec2 v=b-a;return length(p-a-v*clamp(dot(p-a,v)/dot(v,v),0.0,1.0));}
vec3 focusScene(vec2 uv) {
    float t=clockTime*motion,aspect=resolution.x/resolution.y;
    vec2 cityUV=photoCover(uv,2.0/3.0,.82)+tilt*.023;
    vec3 col=cityPhoto(cityUV,1.0)*.86;
    vec2 p=(uv-.5)*vec2(aspect,1.0);float scale=min(aspect/.60,1.55);
    vec2 movement=tilt*vec2(.095,.075)+vec2(sin(t*.17)*.007,cos(t*.13)*.004);
    vec2 g=rotate(tilt.x*.07)*(p-movement-vec2(0,-.025))/scale;
    float arms=min(segmentDistance(g,vec2(-.264,.025),vec2(-.320,.115)),segmentDistance(g,vec2(.264,.025),vec2(.320,.115)));
    float arm=1.0-smoothstep(.010,.016,arms);col=mix(col,vec3(.022,.025,.029),arm);
    float bridge=(1.0-smoothstep(.005,.011,abs(g.y-(.034+.012*cos(g.x*45.0)))))*(1.0-smoothstep(.026,.040,abs(g.x)));
    col=mix(col,vec3(.030,.027,.026),bridge);
    for(int i=0;i<2;i++) {
        float side=float(i)*2.0-1.0;vec2 q=g-vec2(side*.145,0);q.y+=q.x*side*.045;q.x*=1.0+q.y*.42;
        float sd=roundedBox(q,vec2(.122,.095),.041),lens=1.0-smoothstep(-.003,.001,sd);
        vec2 lensUV=cityUV+q*vec2(.014,.016)+tilt*.002;
        vec3 clear=cityPhoto(lensUV,0.0)*vec3(.98,1.0,1.025);float edge=1.0-smoothstep(0.0,.020,-sd);
        vec3 reflected=skyPhoto(clamp(.5+q*1.5+tilt*.12,.02,.98));clear=mix(clear,reflected,.018+edge*.085);
        float streak=exp(-pow((q.x+.65*q.y+.06+sin(t*.15)*.014)*140.0,2.0));clear+=vec3(.11,.16,.20)*streak*.32;
        col=mix(col,clear,lens);
        float rim=(1.0-smoothstep(.014,.018,abs(sd-.004)))*smoothstep(-.004,.002,sd),bevel=exp(-pow((sd-.013)*250.0,2.0));
        vec3 acetate=vec3(.018,.015,.014)+vec3(.08,.066,.051)*(.5+.5*side*q.x*5.0);
        acetate+=vec3(.22,.26,.29)*bevel*(.12+.88*smoothstep(-.06,.06,q.y));col=mix(col,acetate,rim);
        float hinge=1.0-smoothstep(.002,.005,length(q-vec2(side*.116,.034)));col=mix(col,vec3(.64,.65,.61),hinge);
    }
    return col;
}
// Integer-domain hash prevents sin/fract reassociation from moving sites at tile boundaries.
float iceHash(vec2 p) {vec2 a=mod(p,64.0);float n=mod(a.x*127.0+a.y*311.0+19.0,1021.0);return mod(n*n*13.0+n*17.0,1021.0)/1021.0;}
vec3 icePhoto(vec2 p) {return texture2D(iceMaterial,clamp(p,.004,.996)).rgb;}
vec3 iceScene(vec2 uv) {
    float t=clockTime*motion,aspect=resolution.x/resolution.y;
    vec2 p=(uv-.5)*vec2(aspect,1.0)*5.1+tilt*.17,cell=floor(p);
    float closest=100.0;vec2 chosen=vec2(0),center=vec2(0);
    for(int y=-2;y<=2;y++)for(int x=-2;x<=2;x++) {
        vec2 id=cell+vec2(float(x),float(y));
        vec2 point=id+.15+.70*vec2(iceHash(id+vec2(3,17)),iceHash(id+vec2(17,8)));
        float d=dot(p-point,p-point);if(d<closest){closest=d;chosen=id;center=point;}
    }
    float seed=iceHash(chosen+vec2(8,1));
    vec2 drift=vec2(sin(t*.25+seed*7.0),cos(t*.21+seed*11.0))*.018;
    vec2 local=rotate(sin(t*.18+seed*9.0)*.008)*(p-center-drift),edgeNormal=vec2(0);
    float nearest=-100.0;
    // Fixed irregular Voronoi polygons drift inside their water gaps as rigid floes.
    for(int y=-2;y<=2;y++)for(int x=-2;x<=2;x++) {
        if(x!=0||y!=0){vec2 id=chosen+vec2(float(x),float(y));
            vec2 neighbor=id+.15+.70*vec2(iceHash(id+vec2(3,17)),iceHash(id+vec2(17,8)));
            vec2 v=neighbor-center;float lengthV=length(v);vec2 n=v/lengthV;
            float distance=dot(local,n)-lengthV*.5+.038+.008*seed;
            if(distance>nearest){nearest=distance;edgeNormal=n;}
        }
    }
    nearest+=(noise(local*18.0+chosen)-.5)*.006;
    vec2 waves=vec2(sin(p.y*4.0+t*.44),cos(p.x*4.6-t*.37))*.015;
    vec3 water=mix(vec3(.018,.13,.19),vec3(.08,.30,.38),.5+.5*sin(p.y*.85+p.x*.5));
    float ripple=pow(.5+.5*sin(p.x*12.0+p.y*8.0+sin(p.y*7.0+t*.51)+t*.40),14.0);
    water+=vec3(.06,.13,.15)*ripple;water+=skyPhoto(.5+(uv-.5)*.32+waves)*.11;
    water*=1.0-.35*exp(-max(nearest,0.0)*18.0)*shadowEnabled;
    float mask=1.0-smoothstep(-.008,.008,nearest);
    vec2 tex=.5+local*.26+(vec2(iceHash(chosen+vec2(12,7)),iceHash(chosen+vec2(5,6)))-.5)*.35+tilt*.017;
    vec3 material=icePhoto(tex);float height=.18+.08*seed;
    vec3 underneath=icePhoto(tex+tilt*height*.08+vec2(.008,.012));
    vec3 ice=mix(material,underneath,.24)*mix(vec3(.75,.92,.99),vec3(.96,1.04,1.07),seed);
    float bevel=exp(-max(0.0,-nearest)*31.0),below=1.0-smoothstep(.008,.070,nearest+.04+dot(edgeNormal,vec2(.015,-.022)));
    ice=mix(water*vec3(.65,1.10,1.35),ice,.84+.16*below);
    ice+=vec3(.22,.34,.39)*bevel*(.45+.55*dot(edgeNormal,normalize(vec2(-.6,.8))));
    float shine=pow(max(0.0,.60+.30*sin(local.x*3.0+local.y*2.0+t*.22+seed*9.0)),18.0);ice+=vec3(.10,.16,.18)*shine;
    return mix(water,ice,mask);
}
vec3 lavaPhoto(vec2 uv) {vec2 q=clamp(uv,.003,.997);return texture2D(lavaMaterial,vec2(q.x,1.0-q.y)).rgb;}
float molten(vec3 color){return smoothstep(.12,.62,color.r-max(color.b,color.g*.42));}
vec3 lavaScene(vec2 uv) {
    float t=clockTime*motion;vec2 q=photoCover(uv,2.0/3.0,.80)+tilt*.015;
    for(int i=0;i<3;i++){vec3 heightSample=lavaPhoto(q);float h=(1.0-molten(heightSample))*.65+dot(heightSample,vec3(.21,.72,.07))*.15;q+=tilt*h*.009;}
    vec3 base=lavaPhoto(q);float heat=molten(base),phase=fract(t*.045),other=fract(phase+.5),blend=abs(phase-.5)*2.0;
    vec2 flow=vec2(sin(q.y*18.0+t*.18)*.005,.032);vec3 a=lavaPhoto(q-flow*phase),b=lavaPhoto(q-flow*other),moving=mix(a,b,blend);
    vec3 col=mix(base,moving,heat*.45);
    float folds=.5+.5*sin(q.y*115.0-t*2.1+sin(q.x*85.0+t*.43)*1.9),pulse=.88+.12*sin(t*.54+q.y*12.0);
    col+=vec3(.16,.055,.004)*heat*folds*pulse;
    float nearHeat=molten(lavaPhoto(q+vec2(.006,0)))+molten(lavaPhoto(q-vec2(.006,0)))+molten(lavaPhoto(q+vec2(0,.006)))+molten(lavaPhoto(q-vec2(0,.006)));
    col+=vec3(.043,.010,.001)*nearHeat*(1.0-heat)*(.8+.2*sin(t*.61+q.x*8.0));return min(col,vec3(1.0));
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
    else if(mode<6.5) col=reef(uv);
    else if(mode<7.5) col=focusScene(uv);
    else if(mode<8.5) col=iceScene(uv);
    else col=lavaScene(uv);
    // A gentle vignette keeps labels legible without flattening the scene.
    float vignette=1.0-.24*pow(length((uv-.5)*vec2(.8,1.0)),1.4);
    col*=vignette;
    col*=1.0-.20*smoothstep(.70,1.0,uv.y);
    gl_FragColor=vec4(col,1.0);
}
