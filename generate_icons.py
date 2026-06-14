"""生成 PWA 图标（简单的大蒜主题图标）"""
from PIL import Image, ImageDraw
import os

def create_icon(size, path):
    """创建简单的蒜形图标"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 圆形背景（深绿色）
    margin = size // 8
    draw.ellipse([margin, margin, size - margin, size - margin],
                 fill=(46, 125, 50, 255))  # #2e7d32

    # 画简单的大蒜形状（白色）
    center = size // 2
    # 主体椭圆
    body_top = size // 3
    body_bottom = size - size // 5
    body_width = size // 5
    draw.ellipse([center - body_width, body_top, center + body_width, body_bottom],
                 fill=(255, 255, 255, 255))

    # 顶部
    draw.ellipse([center - body_width//2, size//6, center + body_width//2, body_top + size//10],
                 fill=(255, 255, 255, 255))

    # 根须
    root_y = body_bottom + size // 20
    for i in range(3):
        x = center - size//12 + i * size//12
        draw.line([(center, body_bottom), (x, root_y)],
                  fill=(255, 255, 255, 255), width=max(2, size // 50))

    img.save(path, 'PNG')
    print(f'已创建: {path} ({size}x{size})')


if __name__ == '__main__':
    output_dir = os.path.join(os.path.dirname(__file__), 'app', 'icons')
    os.makedirs(output_dir, exist_ok=True)

    create_icon(192, os.path.join(output_dir, 'icon-192.png'))
    create_icon(512, os.path.join(output_dir, 'icon-512.png'))

    print('图标生成完成！')
