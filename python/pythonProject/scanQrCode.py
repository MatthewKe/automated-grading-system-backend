import sys
from enum import Enum
from pyzbar.pyzbar import decode
from PIL import Image


class AnswerAreaContainerSize(Enum):
    Three = (458.825, 989.213)
    Two = (707.638, 989.213)


def scan_qr_code(image_path):
    img = Image.open(image_path)
    qr_codes = decode(img)
    return qr_codes[0].data.decode('utf-8')


def main():
    image_path = sys.argv[1]
    qr_code_info = scan_qr_code(image_path)
    print(qr_code_info)


if __name__ == '__main__':
    main()
