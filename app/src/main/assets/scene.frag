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
// Celestial bank aliases: slots 3/4/6/7 are replaced together by SceneSurface.
#define opticalFrame jellyfish
#define alpineForeground glassSky
#define celestialMaterial iceMaterial
#define celestialSky lavaMaterial
vec2 photoCover(vec2 uv,float sourceAspect,float overscan);

float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
float noise(vec2 p) {
    vec2 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+vec2(1,1)),f.x),f.y);
}
float fbm(vec2 p) { return noise(p)*.55+noise(p*2.1)*.25+noise(p*4.3)*.13; }
float ellipse(vec2 p, vec2 scale) { return length(p/scale); }
mat2 rotate(float a) { return mat2(cos(a),-sin(a),sin(a),cos(a)); }

// Spatial bank: a real distant lake and an independently displaced near shoreline.
vec3 spatialPhoto(vec2 q) {return texture2D(glassSky,vec2(clamp(q.x,.003,.997),1.0-clamp(q.y,.003,.997))).rgb;}
vec3 spatialScene(vec2 uv) {
    float t=clockTime*motion,aspect=resolution.x/resolution.y;
    vec2 q=photoCover(uv,1.5,.88)+tilt*.014;
    float water=1.0-smoothstep(.34,.415,q.y);
    vec2 flow=vec2(sin(q.y*74.0+t*.42+sin(q.x*13.0))*.0010,cos(q.x*39.0-t*.31+q.y*27.0)*.00065)*water*motion;
    vec3 col=spatialPhoto(q+flow)*vec3(.88,.94,.97);
    // Fine reflected wave glints follow the photographic water, leaving mountains still.
    float glint=sin(q.y*220.0+sin(q.x*29.0-t*.24)*1.7+t*.48);
    col+=vec3(.018,.022,.023)*glint*water*motion;
    vec2 screen=resolution/min(resolution.x,resolution.y);
    vec2 movement=tilt*.075+vec2(sin(t*.14)*.007,cos(t*.12)*.004);
    vec2 p=(uv-vec2(.66,.34))*screen-movement;
    float radius=.158,r=length(p)/radius;
    vec2 shadow=(p-vec2(0,-.155))/vec2(.18,.025);
    col*=1.0-exp(-dot(shadow,shadow))*water*.16*shadowEnabled;
    if(r<1.015) {
        vec3 n=normalize(vec3(p/radius,sqrt(max(.0001,1.0-r*r))));
        vec2 through=q+flow-n.xy*(.035+.018*n.z);
        vec3 clear=spatialPhoto(through);
        clear.r=spatialPhoto(through+n.xy*.00065).r;
        clear.b=spatialPhoto(through-n.xy*.00065).b;
        vec3 reflected=spatialPhoto(vec2(.5+n.x*n.z*.80,.56+n.y*n.z*.72));
        float fresnel=.045+.955*pow(1.0-n.z,5.0);
        vec3 glass=mix(clear*vec3(.88,.97,1.0),reflected,.10+fresnel*.84);
        glass*=.88+.12*smoothstep(-1.0,.3,n.y);
        float shine=pow(max(0.0,dot(n,normalize(vec3(-.45,.58,.70)))),95.0);
        glass+=vec3(.93,.87,.76)*shine*.75;
        glass+=vec3(.18,.27,.29)*pow(1.0-n.z,9.0)*.38;
        col=mix(col,glass,1.0-smoothstep(.995,1.012,r));
    }
    float cropY=min(1.0,1.0/aspect);
    vec2 nearUV=vec2(.5+(uv.x-.5)*min(1.0,aspect)*.84,.72+.22*cropY-uv.y*cropY*.95)+vec2(tilt.x*.045,-tilt.y*.026);
    if(nearUV.y>.002) {
        vec4 shore=texture2D(jellyfish,clamp(nearUV,vec2(.003),vec2(.997)));
        col=mix(col,shore.rgb*.77,min(1.0,shore.a*1.025));
    }
    return col;
}

vec3 starEnvironment(vec2 uv) {
    float t=clockTime*motion;
    vec2 q=photoCover(uv,2.0,.86)+tilt*.009+vec2(sin(t*.012)*.012,0);
    vec3 col=texture2D(celestialSky,vec2(clamp(q.x,.002,.998),clamp(1.0-q.y,.004,.996)*.5)).rgb;
    // A sparse nearer layer moves separately from the photographed distant sky.
    vec2 p=(uv-.5)*resolution/min(resolution.x,resolution.y);
    for(int layer=0;layer<2;layer++) {
        float k=float(layer)+1.0;vec2 s=(p+tilt*.012*k)*48.0/k;
        vec2 cell=floor(s),f=fract(s)-.5;
        float star=(1.0-smoothstep(.012,.070,length(f)))*step(.992,hash(cell+k*13.0));
        col+=vec3(.68,.78,.92)*star*(.55+.10*sin(t*.35+hash(cell)*6.28));
    }
    return col;
}

vec3 spaceBackdrop(vec2 uv) {
    // Fit the long side of the sky photograph to the long side of the display.
    // In portrait this uses the atlas's 2048-pixel axis down the phone.
    bool portrait = resolution.y > resolution.x;
    vec2 display = portrait ? resolution.yx : resolution;
    vec2 view = portrait ? uv.yx : uv;
    vec2 pan = portrait ? tilt.yx : tilt;
    float t = clockTime * motion;
    float aspect = display.x / display.y;
    vec2 crop = min(vec2(1.0), vec2(aspect * .5, 2.0 / aspect));
    vec2 sky = .5 + (view - .5) * crop * .94 + pan * .008;
    sky.x += sin(t * .012) * .009;
    vec2 atlas = vec2(sky.x, (1.0 - sky.y) * .5);
    vec3 photo = texture2D(celestialSky, atlas).rgb;

    // Recover local contrast in enlarged photographic stars while keeping dark dust smooth.
    vec2 texel = vec2(1.0 / 2048.0, 0.0);
    vec3 nearby = (texture2D(celestialSky, atlas + texel.xy).rgb
                 + texture2D(celestialSky, atlas - texel.xy).rgb
                 + texture2D(celestialSky, atlas + texel.yx).rgb
                 + texture2D(celestialSky, atlas - texel.yx).rgb) * .25;
    vec3 skyColor = photo + .65 * (photo - nearby);

    // Stable, subpixel-positioned points add a nearer layer. Evaluate their
    // footprint in display pixels so small movements keep continuous coverage.
    float shortSide = min(resolution.x, resolution.y);
    float spacing = max(10.0, shortSide / 72.0);
    vec2 grid = ((uv - .5) * resolution + tilt * shortSide * .021) / spacing;
    vec2 cell = floor(grid);
    float magnitude = hash(cell);
    vec2 center = .22 + .56 * vec2(hash(cell + 11.7), hash(cell + 37.1));
    float distancePx = length(fract(grid) - center) * spacing;
    float radius = clamp(shortSide / 1080.0, .70, 1.5) * mix(.60, 1.15, magnitude);
    float core = 1.0 - smoothstep(max(0.0, radius - .65), radius + .65, distancePx);
    float light = smoothstep(.80, 1.0, magnitude);
    skyColor += core * light * mix(vec3(.56,.66,.83), vec3(.94,.87,.76), center.x);
    return max(skyColor, vec3(0.0));
}

vec3 hologram(vec2 uv) {
    float t=clockTime*motion;vec3 col=spaceBackdrop(uv);
    vec2 aspect=resolution/min(resolution.x,resolution.y);
    vec2 p=(uv-vec2(.56,.60))*aspect-tilt*.055;
    float radius=.31,r=length(p)/radius,edge=1.0-smoothstep(.995,1.006,r);
    if(r<1.006) {
        vec3 n=normalize(vec3(p/radius,sqrt(max(.00001,1.0-r*r))));
        vec2 map=vec2(fract(atan(n.z,n.x)/6.283185+.5+t*.0022),acos(clamp(n.y,-1.0,1.0))/3.141593);
        vec3 albedo=texture2D(celestialMaterial,vec2(map.x,.002+map.y*.496)).rgb;
        vec3 light=normalize(vec3(-.85,.65,.75));float diffuse=max(0.0,dot(n,light));
        float ocean=smoothstep(.04,.15,albedo.b-max(albedo.r,albedo.g));
        float specular=pow(max(0.0,dot(n,normalize(light+vec3(0,0,1)))),65.0)*ocean;
        float rim=pow(1.0-n.z,3.0)*(.25+.75*diffuse);
        vec3 surface=albedo*(.035+.965*diffuse)+vec3(.6,.76,.9)*specular*.38+vec3(.07,.25,.47)*rim;
        col=mix(col,surface,edge);
    }
    float atmosphere=exp(-max(0.0,r-1.0)*85.0)*smoothstep(.97,1.008,r);
    col+=vec3(.035,.12,.24)*atmosphere*(.35+.65*smoothstep(-.3,.3,p.y-p.x));
    return col;
}
vec3 asteroidField(vec2 uv) {
    float t=clockTime*motion;vec3 col=spaceBackdrop(uv);
    vec2 aspect=resolution/min(resolution.x,resolution.y),p=(uv-.5)*aspect;
    for(int i=0;i<5;i++) {
        float k=float(i);vec2 center;float size;
        if(i==0){center=vec2(.24,.82);size=.080;}
        else if(i==1){center=vec2(.76,.67);size=.14;}
        else if(i==2){center=vec2(.27,.39);size=.19;}
        else if(i==3){center=vec2(.78,.20);size=.10;}
        else {center=vec2(.49,.57);size=.045;}
        vec2 drift=vec2(sin(t*(.025+k*.004)+k),cos(t*(.021+k*.003)+k))*vec2(.012,.018);
        vec2 q=rotate(k*1.9+t*(.012+k*.002))*(p-(center-.5)*aspect-tilt*(.018+size*.32)-drift)/size;
        q/=vec2(1.0,.84+k*.025);
        float angle=atan(q.y,q.x),shape=1.0+.075*sin(angle*3.0+k*2.0)+.035*sin(angle*7.0-k);
        float r=length(q)/shape;
        if(r<1.018) {
            vec3 n=normalize(vec3(q/shape,sqrt(max(.0001,1.0-r*r))));
            vec3 mapped=n;mapped.xz=rotate(t*(.018+k*.003)+k)*mapped.xz;
            vec2 map=vec2(fract(atan(mapped.z,mapped.x)/6.283185+.5),acos(clamp(mapped.y,-1.0,1.0))/3.141593);
            vec2 tex=vec2(.002+map.x*.496,.502+map.y*.496);
            vec3 rock=texture2D(celestialMaterial,tex).rgb;
            float dx=dot(texture2D(celestialMaterial,tex+vec2(.0015,0)).rgb-texture2D(celestialMaterial,tex-vec2(.0015,0)).rgb,vec3(.333));
            float dy=dot(texture2D(celestialMaterial,tex+vec2(0,.0015)).rgb-texture2D(celestialMaterial,tex-vec2(0,.0015)).rgb,vec3(.333));
            vec3 normal=normalize(n+vec3(-dx*1.3,dy*1.3,0));
            float diffuse=max(0.0,dot(normal,normalize(vec3(-.65,.8,1.0))));
            vec3 surface=rock*vec3(.96,.94,.91)*(.035+diffuse*1.18);
            col=mix(col,surface,1.0-smoothstep(.995,1.012,r));
        }
    }
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
    vec3 col=cityPhoto(cityUV,1.0)*.94;
    vec2 p=(uv-.5)*vec2(aspect,1.0);float scale=min(aspect*.96,.95);
    vec2 movement=tilt*vec2(.075,.065)+vec2(sin(t*.17)*.006,cos(t*.13)*.003);
    vec2 q=rotate(tilt.x*.055)*(p-movement)/scale+vec2(.5);
    if(q.x>.002&&q.x<.998&&q.y>.002&&q.y<.998) {
        vec2 tex=vec2(q.x*.5,1.0-q.y);
        vec4 frame=texture2D(opticalFrame,tex);
        vec2 mask=texture2D(opticalFrame,tex+vec2(.5,0)).rg;
        float edge=1.0-mask.g;
        vec3 clear=cityPhoto(cityUV+(q-.5)*(.0015+edge*.003)+tilt*.001,0.0)*vec3(.98,1.0,1.015);
        float reflection=exp(-pow((q.x+q.y*.55-.67+tilt.x*.035+sin(t*.15)*.012)*70.0,2.0));
        clear+=vec3(.10,.14,.18)*(reflection*.35+edge*.08);
        col=mix(col,clear,mask.r);
        col=mix(col,frame.rgb,frame.a);
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
    float t=clockTime*motion;vec3 col=starEnvironment(uv)*.65+vec3(.003,.009,.017);
    vec2 light=photoCover(uv,1.25,.80)+tilt*.020;
    light.x+=sin(light.y*5.0+t*.16)*.035+sin(t*.07)*.025;
    light.y+=sin(light.x*8.0-t*.12)*.025+sin(light.x*21.0+t*.20)*.007;
    vec3 curtain=texture2D(celestialSky,vec2(clamp(light.x,.003,.997),.502+clamp(1.0-light.y,.003,.997)*.496)).rgb;
    float breathing=.82+.10*sin(t*.21)+.08*sin(light.x*9.0-t*.28);
    col+=curtain*breathing*.88;
    // Portrait keeps the whole ridge; wide screens show additional terrain width.
    float aspect=resolution.x/resolution.y;
    vec2 terrain=vec2(.5+(uv.x-.5)*min(1.0,aspect)*.92,1.0-uv.y*.88)+vec2(tilt.x*.035,-tilt.y*.025);
    vec4 mountain=texture2D(alpineForeground,clamp(terrain,vec2(.003),vec2(.997)));
    return mix(col,mountain.rgb*.76,min(1.0,mountain.a*1.025));
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
    if(mode<.5) col=spatialScene(uv);
    else if(mode<1.5) col=gravityScene(uv);
    else if(mode<2.5) col=hologram(uv);
    else if(mode<3.5) col=pond(uv);
    else if(mode<4.5) col=atrium(uv);
    else if(mode<5.5) col=aurora(uv);
    else if(mode<6.5) col=reef(uv);
    else if(mode<7.5) col=focusScene(uv);
    else if(mode<8.5) col=iceScene(uv);
    else if(mode<9.5) col=lavaScene(uv);
    else col=asteroidField(uv);
    // A gentle vignette keeps labels legible without flattening the scene.
    float vignette=1.0-.24*pow(length((uv-.5)*vec2(.8,1.0)),1.4);
    col*=vignette;
    col*=1.0-.20*smoothstep(.70,1.0,uv.y);
    gl_FragColor=vec4(col,1.0);
}
