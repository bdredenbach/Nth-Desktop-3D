"""Prepare generated source images for the GLES2 eight-sampler texture budget.

Input paths/prompts are recorded in docs/scene-087-assets.json. No creative edits:
resize/pad, extract enclosed lens alpha, pack registered images and data masks.
"""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageFilter
from scipy.ndimage import label, distance_transform_edt

root = Path(__file__).resolve().parents[1]
manifest_path = root / 'docs/scene-087-assets.json'
manifest = json.loads(manifest_path.read_text())
dest = root / 'app/src/main/res/drawable-nodpi'
sources = {}
for name, path in manifest['paths'].items():
    sources[name] = Image.open(path).convert('RGBA')
    manifest.setdefault('source_metadata', {})[name] = {
        'sha256': hashlib.sha256(Path(path).read_bytes()).hexdigest(),
        'size': list(sources[name].size)}

def save(im, name, lossless=False):
    im.save(dest / (name + '.webp'), 'WEBP', quality=93, method=6, lossless=lossless)

save(sources['alpine_foreground'].resize((1536,1536), Image.Resampling.LANCZOS), 'alpine_foreground')
material = Image.new('RGB', (2048,2048))
material.paste(sources['planet_surface'].resize((2048,1024), Image.Resampling.LANCZOS), (0,0))
rock = sources['asteroid_regolith'].resize((1024,1024), Image.Resampling.LANCZOS)
material.paste(rock,(0,1024)); material.paste(rock,(1024,1024))
save(material, 'celestial_material')
sky = Image.new('RGB',(2048,2048))
sky.paste(sources['star_environment'].resize((2048,1024), Image.Resampling.LANCZOS),(0,0))
sky.paste(sources['aurora_light'].resize((2048,1024), Image.Resampling.LANCZOS),(0,1024))
save(sky, 'celestial_sky')

# A front-facing photographic frame. Empty enclosed alpha components are its lenses.
original=sources['optical_frame']; original.thumbnail((1000,1000),Image.Resampling.LANCZOS)
frame=Image.new('RGBA',(1024,1024)); frame.alpha_composite(original,((1024-original.width)//2,(1024-original.height)//2))
alpha=np.asarray(frame)[:,:,3]
components,n=label(alpha<64)
border=set(np.unique(np.concatenate([components[0],components[-1],components[:,0],components[:,-1]])))
sizes=np.bincount(components.ravel())
holes=sorted([i for i in range(1,n+1) if i not in border],key=lambda i:-sizes[i])[:2]
assert len(holes)==2 and min(sizes[i] for i in holes)>10000, 'Generated frame must have two transparent lens openings'
mask=np.isin(components,holes); distance=distance_transform_edt(mask)
data=np.zeros((1024,1024,4),np.uint8)
data[:,:,0]=np.minimum(distance/1.5,1)*255
data[:,:,1]=np.minimum(distance/24,1)*255
data[:,:,3]=255
atlas=Image.new('RGBA',(2048,1024));atlas.paste(frame,(0,0));atlas.paste(Image.fromarray(data),(1024,0))
save(atlas,'optical_frame',True)

# Keep the exact sharp photograph and registration; soften the background only mildly.
city=Image.open(dest/'focus_city.webp').convert('RGB'); sharp=city.crop((0,0,city.width//2,city.height))
city.paste(sharp,(0,0));city.paste(sharp.filter(ImageFilter.GaussianBlur(4.5)),(city.width//2,0))
save(city,'focus_city')
manifest['packing']={
 'alpine_foreground':'RGBA terrain; transparent sky',
 'celestial_material':'2048 square: planet 2048x1024 top; duplicated rock 1024x1024 bottom',
 'celestial_sky':'2048 square: stars 2048x1024 top; aurora 2048x1024 bottom',
 'optical_frame':'2048x1024: square RGBA frame left; R=lens mask, G=normalized interior edge distance right',
 'focus_city':'Registered sharp left / Gaussian blur radius 4.5 right; same original city photograph'}
manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
print('Packed four celestial/optical resources and reduced city blur.')
