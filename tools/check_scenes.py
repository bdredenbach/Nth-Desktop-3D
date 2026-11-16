"""Compile and render the shipped scene shader on a surfaceless EGL/GLES context.
Linux development check; needs Mesa EGL and Pillow. Does not replace device tests.
"""
import ctypes as C
import json
import os
from pathlib import Path
import sys
os.environ.setdefault('EGL_PLATFORM', 'surfaceless')
os.environ.setdefault('LIBGL_ALWAYS_SOFTWARE', '1')
from PIL import Image

root = Path(__file__).resolve().parents[1]
out = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'build/scene-check'
out.mkdir(parents=True, exist_ok=True)
e = C.CDLL('libEGL.so.1')
I, F, P, U = C.c_int, C.c_float, C.c_void_p, C.c_uint

def egl(name, result, args):
    f = getattr(e, name); f.restype = result; f.argtypes = args; return f
get_display = egl('eglGetDisplay', P, [P])
initialize = egl('eglInitialize', I, [P, C.POINTER(I), C.POINTER(I)])
choose = egl('eglChooseConfig', I, [P, C.POINTER(I), C.POINTER(P), I, C.POINTER(I)])
create_surface = egl('eglCreatePbufferSurface', P, [P, P, C.POINTER(I)])
create_context = egl('eglCreateContext', P, [P, P, P, C.POINTER(I)])
make_current = egl('eglMakeCurrent', I, [P, P, P, P])
get_proc = egl('eglGetProcAddress', P, [C.c_char_p])
d = get_display(None); major, minor = I(), I()
assert initialize(d, C.byref(major), C.byref(minor)), 'EGL initialization failed'
config, count = P(), I()
assert choose(d, (I*13)(0x3024,8,0x3023,8,0x3022,8,0x3033,1,0x3040,4,0x3038,0,0), C.byref(config), 1, C.byref(count)) and count.value
width, height = 540, 1000
surface = create_surface(d, config, (I*5)(0x3057,width,0x3056,height,0x3038))
context = create_context(d, config, None, (I*3)(0x3098,2,0x3038))
assert context and surface and make_current(d,surface,surface,context), 'GLES context failed'

def gl(name, result, *args):
    address = get_proc(name.encode()); assert address, name
    return C.CFUNCTYPE(result, *args)(address)
get_string=gl('glGetString', C.c_char_p, U)
create_shader=gl('glCreateShader', U,U)
shader_source=gl('glShaderSource',None,U,I,C.POINTER(C.c_char_p),C.POINTER(I))
compile_shader=gl('glCompileShader',None,U)
get_shader=gl('glGetShaderiv',None,U,U,C.POINTER(I))
shader_log=gl('glGetShaderInfoLog',None,U,I,C.POINTER(I),C.c_char_p)
create_program=gl('glCreateProgram',U)
attach=gl('glAttachShader',None,U,U)
link=gl('glLinkProgram',None,U)
get_program=gl('glGetProgramiv',None,U,U,C.POINTER(I))
use=gl('glUseProgram',None,U)
attribute=gl('glGetAttribLocation',I,U,C.c_char_p)
uniform=gl('glGetUniformLocation',I,U,C.c_char_p)
u1=gl('glUniform1f',None,I,F);u2=gl('glUniform2f',None,I,F,F);u3v=gl('glUniform3fv',None,I,I,C.POINTER(F))
vertex=gl('glVertexAttribPointer',None,U,I,U,U,I,P)
enable=gl('glEnableVertexAttribArray',None,U)
viewport=gl('glViewport',None,I,I,I,I)
draw=gl('glDrawArrays',None,U,I,I)
read=gl('glReadPixels',None,I,I,I,I,U,U,P)
error=gl('glGetError',U)
finish=gl('glFinish',None)

def shader(kind, text):
    q=create_shader(kind); src=C.c_char_p(text.encode());shader_source(q,1,C.byref(src),None);compile_shader(q)
    ok=I();get_shader(q,0x8B81,C.byref(ok))
    if not ok.value:
        msg=C.create_string_buffer(10000);shader_log(q,10000,None,msg);raise AssertionError(msg.value.decode())
    return q
vs='attribute vec2 position;void main(){gl_Position=vec4(position,0,1);}'
p=create_program();attach(p,shader(0x8B31,vs));attach(p,shader(0x8B30,(root/'app/src/main/assets/scene.frag').read_text()));link(p)
ok=I();get_program(p,0x8B82,C.byref(ok));assert ok.value,'Program link failed'
use(p)
gen_textures=gl('glGenTextures',None,I,C.POINTER(U));active_texture=gl('glActiveTexture',None,U)
bind_texture=gl('glBindTexture',None,U,U);texture_parameter=gl('glTexParameteri',None,U,U,I)
texture_image=gl('glTexImage2D',None,U,I,I,I,I,I,U,U,P);u1i=gl('glUniform1i',None,I,I)
textures=(U*8)();gen_textures(8,textures)
for unit,(name,filename) in enumerate([('pondBed','pond_bed'),('koiFish','koi_fish'),('reefWater','reef_water'),('jellyfish','jellyfish'),('glassSky','glass_sky'),('focusCity','focus_city'),('iceMaterial','ice_material'),('lavaMaterial','lava_material')]):
    image=Image.open(root/f'app/src/main/res/drawable-nodpi/{filename}.webp').convert('RGBA')
    active_texture(0x84C0+unit);bind_texture(0x0DE1,textures[unit])
    for key,value in [(0x2801,0x2601),(0x2800,0x2601),(0x2802,0x812F),(0x2803,0x812F)]:texture_parameter(0x0DE1,key,value)
    data=(C.c_ubyte*(image.width*image.height*4)).from_buffer_copy(image.tobytes())
    texture_image(0x0DE1,0,0x1908,image.width,image.height,0,0x1908,0x1401,C.cast(data,P))
    u1i(uniform(p,name.encode()),unit)
quad=(F*8)(-1,-1,1,-1,-1,1,1,1);a=attribute(p,b'position');enable(a);vertex(a,2,0x1406,0,0,C.cast(quad,P));viewport(0,0,width,height)
u2(uniform(p,b'resolution'),width,height)
u3v(uniform(p,b'touches[0]'),4,(F*12)(0,0,-100,0,0,-100,0,0,-100,0,0,-100))
for name,value in [('clockTime',20),('motion',1),('fishCount',5),('fishSpeed',.6),('rippleEnabled',1),('shadowEnabled',1),('waterLight',.75)]:u1(uniform(p,name.encode()),value)
results=[]
for mode in range(10):
    u1(uniform(p,b'mode'),mode);u2(uniform(p,b'tilt'),0,0);draw(5,0,4);finish();assert error()==0
    pixels=(C.c_ubyte*(width*height*4))();read(0,0,width,height,0x1908,0x1401,pixels)
    image=Image.frombytes('RGBA',(width,height),bytes(pixels)).transpose(Image.Transpose.FLIP_TOP_BOTTOM).convert('RGB')
    assert len(image.getcolors(width*height) or [])>100,'Scene is blank or uniform'
    image.save(out/f'mode-{mode}.png');results.append({'mode':mode,'center':image.getpixel((width//2,height//2))})
# New scenes must animate, freeze with ambient motion disabled, and respond to tilt.
for mode in range(3,10):
    def frame(time, moving, x=0, y=0):
        u1(uniform(p,b'mode'),mode);u1(uniform(p,b'clockTime'),time);u1(uniform(p,b'motion'),moving);u2(uniform(p,b'tilt'),x,y)
        draw(5,0,4);finish();assert error()==0
        pixels=(C.c_ubyte*(width*height*4))();read(0,0,width,height,0x1908,0x1401,pixels)
        return bytes(pixels)
    paused=frame(2,0)
    assert paused==frame(45,0),'Reduced-motion scene is still animating'
    assert frame(2,1)!=frame(45,1),'Ambient scene is static'
    assert paused!=frame(2,0,.6,-.3),'Parallax scene ignores tilt'
# Fish controls change the textured animals, rather than only changing their caption.
u1(uniform(p,b'mode'),3);u1(uniform(p,b'motion'),1);u1(uniform(p,b'clockTime'),20);u2(uniform(p,b'tilt'),0,0)
def aquatic_frame():
    draw(5,0,4);finish();assert error()==0
    data=(C.c_ubyte*(width*height*4))();read(0,0,width,height,0x1908,0x1401,data)
    return bytes(data)
u1(uniform(p,b'fishCount'),3);few=aquatic_frame();u1(uniform(p,b'fishCount'),7);assert few!=aquatic_frame(),'Fish count does not affect rendering'
u1(uniform(p,b'fishCount'),5);u1(uniform(p,b'fishSpeed'),.3);slow=aquatic_frame();u1(uniform(p,b'fishSpeed'),1.2);assert slow!=aquatic_frame(),'Fish speed does not affect rendering'
u1(uniform(p,b'fishSpeed'),.6)
# Isolate new light from animal movement: compare lit/unlit differences at equal times.
import numpy as np
for checked in (3,6):
    u1(uniform(p,b'mode'),checked);u1(uniform(p,b'fishCount'),0);u2(uniform(p,b'tilt'),0,0)
    def light_frame(time,moving,strength):
        u1(uniform(p,b'clockTime'),time);u1(uniform(p,b'motion'),moving);u1(uniform(p,b'waterLight'),strength)
        return np.frombuffer(aquatic_frame(),dtype=np.uint8).reshape(height,width,4)[::-1].astype(np.int16)
    a=light_frame(2,1,.75)-light_frame(2,1,0)
    b=light_frame(10,1,.75)-light_frame(10,1,0)
    region=np.s_[height*3//4:height,:,:3]
    assert np.mean(np.abs(a[region]))>.1,'Rock caustics are invisible'
    assert np.mean(np.abs(a[region]-b[region]))>.1,'Rock caustics do not move'
    assert np.array_equal(light_frame(2,0,.75),light_frame(45,0,.75)),'Paused aquatic light still moves'
    for time in (2,10):
        pixels=light_frame(time,1,.75).clip(0,255).astype(np.uint8)
        Image.fromarray(pixels[:,:,:3]).save(out/f'water-light-{checked}-t{time}.png')
    if checked==6:
        upper=np.s_[height//8:height//2,:,:3]
        assert np.mean(np.abs(a[upper]-b[upper]))>.1,'Jellyfish light shafts do not move'
# Maximum supported camera tilt must retain real texture detail across the top edge.
for checked in (3,6):
    u1(uniform(p,b'mode'),checked);u1(uniform(p,b'motion'),0)
    for tx,ty in ((1.5,1.5),(-1.5,-1.5)):
        u2(uniform(p,b'tilt'),tx,ty)
        pixels=np.frombuffer(aquatic_frame(),dtype=np.uint8).reshape(height,width,4)[::-1].astype(np.int16)
        strip=pixels[2:height//22,:,:3]
        assert np.mean(np.abs(strip[1:]-strip[:-1]))>.35,'Aquatic top edge is stretched at maximum tilt'
        Image.fromarray(pixels[:,:,:3].clip(0,255).astype(np.uint8)).save(out/f'edge-{checked}-{tx}.png')
u1(uniform(p,b'waterLight'),.75);u1(uniform(p,b'fishCount'),5)

# Landscape shader draw without relying on a virtual device rotation.
u2(uniform(p,b'resolution'),height,width)
for mode in range(10):
    u1(uniform(p,b'mode'),mode);draw(5,0,4);finish();assert error()==0
u2(uniform(p,b'resolution'),width,height);u1(uniform(p,b'motion'),1);u1(uniform(p,b'clockTime'),20)
# Exercise tilted pond and an active touch ripple.
u1(uniform(p,b'mode'),3);u2(uniform(p,b'tilt'),.7,-.3);u3v(uniform(p,b'touches[0]'),4,(F*12)(.5,.5,19.7,0,0,-100,0,0,-100,0,0,-100));draw(5,0,4);finish();assert error()==0
pixels=(C.c_ubyte*(width*height*4))();read(0,0,width,height,0x1908,0x1401,pixels)
Image.frombytes('RGBA',(width,height),bytes(pixels)).transpose(Image.Transpose.FLIP_TOP_BOTTOM).save(out/'pond-tilt-ripple.png')
# Compile the exact shipped lit-mesh shaders, too.
import re
source=(root/'app/src/main/java/com/nth/desktop3d/SceneSurface.java').read_text()
strings=re.search(r'meshProgram=link\(("(?:[^"\\]|\\.)*"),\s*("(?:[^"\\]|\\.)*")\)',source)
assert strings,'Cannot locate mesh shaders'
mesh=create_program();attach(mesh,shader(0x8B31,json.loads(strings[1])));attach(mesh,shader(0x8B30,json.loads(strings[2])));link(mesh);get_program(mesh,0x8B82,C.byref(ok));assert ok.value,'Mesh link failed'
print(json.dumps({'renderer':get_string(0x1F01).decode(),'scenes':results,'mesh':'compiled and linked','tilted_pond_ripple':'rendered'}))
