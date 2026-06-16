from PIL import Image, ImageDraw, ImageFont
import os

W, H = 1024, 500
img = Image.new("RGBA", (W, H), (13, 11, 1, 255))
draw = ImageDraw.Draw(img)

# Dark gradient overlay
for y in range(H):
    alpha = int(60 * (1 - y / H))
    draw.rectangle([0, y, W, y], fill=(0, 255, 127, alpha))

# Accent accent line top
draw.rectangle([0, 0, W, 4], fill=(0, 255, 127, 255))

# Gold accent bar bottom
draw.rectangle([0, H - 3, W, H], fill=(255, 215, 0, 200))

# --- Dumbbell icon (simple vector) ---
cx, cy = 150, 220
r_bar = 60
r_w = 14
r_h = 30
# Bar
draw.rectangle([cx - r_bar, cy - 6, cx + r_bar, cy + 6], fill=(0, 255, 127, 220))
# Weight plates
for dx in [-r_bar - 4, -r_bar - 4, r_bar + 4, r_bar + 4]:
    ox = -12 if dx < 0 else 0
    draw.rounded_rectangle([dx + ox, cy - r_h, dx + ox + 14, cy + r_h], radius=4, fill=(255, 215, 0, 220))

# --- "F" letter large ---
try:
    font_large = ImageFont.truetype("C:\\Windows\\Fonts\\arialbd.ttf", 160)
    font_med = ImageFont.truetype("C:\\Windows\\Fonts\\arialbd.ttf", 32)
    font_small = ImageFont.truetype("C:\\Windows\\Fonts\\arial.ttf", 20)
except:
    font_large = ImageFont.load_default()
    font_med = font_large
    font_small = font_large

draw.text((W // 2, 180), "FORMIX", fill=(0, 255, 127, 255), font=font_large, anchor="mm")
draw.text((W // 2, 280), "Fitness & Nutrition Tracker", fill=(255, 215, 0, 230), font=font_med, anchor="mm")
draw.text((W // 2, 330), "Dein intelligenter Trainings- und Ernährungsbegleiter", fill=(200, 200, 200, 200), font=font_small, anchor="mm")

# Bottom tag
draw.text((W // 2, H - 50), "OFFLINE  •  KEINE WERBUNG  •  DATENSCHUTZ FIRST", fill=(180, 180, 180, 180), font=font_small, anchor="mm")

output = os.path.join(os.path.dirname(__file__), "feature_graphic.png")
img.save(output, "PNG")
print(f"Saved: {output} ({os.path.getsize(output)} bytes)")
