"""生成微信小程序 TabBar 图标"""
from PIL import Image, ImageDraw
import os

OUT_DIR = r'D:\毕业设计\front\images'
os.makedirs(OUT_DIR, exist_ok=True)

SIZE = 81
GRAY = '#999999'
BLUE = '#4A90D9'
WHITE = '#FFFFFF'

icons = {
    'tab-study':       ('📖', '学习'),
    'tab-study-active': ('📖', '学习'),
    'tab-checkin':     ('✅', '打卡'),
    'tab-checkin-active': ('✅', '打卡'),
    'tab-rank':        ('🏆', '排行'),
    'tab-rank-active': ('🏆', '排行'),
    'tab-profile':     ('👤', '我的'),
    'tab-profile-active': ('👤', '我的'),
}

for name, (emoji, label) in icons.items():
    is_active = 'active' in name
    color = BLUE if is_active else GRAY

    img = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 画圆形背景
    cx, cy = SIZE // 2, SIZE // 2
    r = 28
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)

    # 尝试画文字代替图标
    from PIL import ImageFont

    # 使用默认字体
    try:
        # 尝试找个中文字体
        font_paths = [
            'C:/Windows/Fonts/msyh.ttc',
            'C:/Windows/Fonts/simhei.ttf',
            'C:/Windows/Fonts/simsun.ttc',
            'C:/Windows/Fonts/arial.ttf',
        ]
        font = None
        for fp in font_paths:
            if os.path.exists(fp):
                font = ImageFont.truetype(fp, 22)
                break
        if font is None:
            font = ImageFont.load_default()
    except:
        font = ImageFont.load_default()

    # 简单图标: 用字母或符号
    char_map = {
        'tab-study': 'S', 'tab-study-active': 'S',
        'tab-checkin': 'C', 'tab-checkin-active': 'C',
        'tab-rank': 'R', 'tab-rank-active': 'R',
        'tab-profile': 'P', 'tab-profile-active': 'P',
    }

    char = char_map[name]
    bbox = draw.textbbox((0, 0), char, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    tx = (SIZE - tw) // 2
    ty = (SIZE - th) // 2 - 2
    draw.text((tx, ty), char, fill=WHITE, font=font)

    path = os.path.join(OUT_DIR, f'{name}.png')
    img.save(path)
    print(f'[OK] {path}')

print('\n全部图标生成完成！')
