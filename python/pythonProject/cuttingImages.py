import json
import os
import sys
from enum import Enum
import cv2 as cv
import numpy as np
from pyzbar.pyzbar import decode
from PIL import Image


class AnswerAreaContainerSize(Enum):
    Three = (458.825, 989.213)
    Two = (707.638, 989.213)


def read_project_config(file_path):
    # 打开并读取JSON文件
    with open(file_path, 'r', encoding='utf-8') as file:
        data = json.load(file)
    return data


def get_rectangle_contours(contours):
    rect_contours = []
    for contour in contours:
        # 计算轮廓的近似值
        epsilon = 0.05 * cv.arcLength(contour, True)
        approx = cv.approxPolyDP(contour, epsilon, True)

        # 如果近似轮廓有四个点，可能是矩形
        if len(approx) == 4:
            rect_contours.append(approx)

    return rect_contours


def merge_close_contours(contours, close_distance=10):
    # 初始化标记数组，标记那些已经被合并的轮廓
    merged = [False] * len(contours)
    new_contours = []

    for i in range(len(contours)):
        if not merged[i]:
            # 获取当前轮廓的外接矩形
            x, y, w, h = cv.boundingRect(contours[i])
            # 初始化合并区域为当前轮廓的区域
            merge_rect = (x, y, x + w, y + h)

            # 查找与当前轮廓靠近的其他轮廓
            for j in range(i + 1, len(contours)):
                if not merged[j]:
                    x2, y2, w2, h2 = cv.boundingRect(contours[j])
                    # 计算轮廓之间的距离
                    distance = np.sqrt((x2 - x) ** 2 + (y2 - y) ** 2)

                    # 如果距离小于阈值，则合并轮廓
                    if distance < close_distance:
                        merged[j] = True
                        merge_rect = (
                            min(merge_rect[0], x2),
                            min(merge_rect[1], y2),
                            max(merge_rect[2], x2 + w2),
                            max(merge_rect[3], y2 + h2)
                        )

            # 添加新的合并后的轮廓
            new_contours.append(
                np.array([
                    [[merge_rect[0], merge_rect[1]]],  # 左上角
                    [[merge_rect[2], merge_rect[1]]],  # 右上角
                    [[merge_rect[2], merge_rect[3]]],  # 右下角
                    [[merge_rect[0], merge_rect[3]]]  # 左下角
                ], dtype=np.int32)
            )

    return new_contours


def get_answer_area_container_size(num_of_answer_area_containers):
    answer_area_container_size = (0, 0)
    if num_of_answer_area_containers == 3:
        answer_area_container_size = AnswerAreaContainerSize.Three
    elif num_of_answer_area_containers == 2:
        answer_area_container_size = AnswerAreaContainerSize.Two

    return answer_area_container_size


def get_answer_area_container_contours(thresh, num_of_answer_area_containers, image):
    # 寻找轮廓
    contours, hierarchy = cv.findContours(thresh, cv.RETR_TREE, cv.CHAIN_APPROX_SIMPLE)
    rect_contours = get_rectangle_contours(contours)

    answer_area_container_size = get_answer_area_container_size(num_of_answer_area_containers)
    answer_area_container_width, answer_area_container_height = answer_area_container_size.value

    answer_area_container_contours = []
    for rect in rect_contours:
        height = rect[2][0][1] - rect[0][0][1]
        width = rect[2][0][0] - rect[0][0][0]
        epsilon = 0.5
        if epsilon > (height / width - (answer_area_container_height / answer_area_container_width)) > -epsilon:
            cv.drawContours(image, [rect], -1, (0, 255, 0), 2)  # 绿色，线宽为2
            answer_area_container_contours.append(rect)

    answer_area_container_contours = merge_close_contours(answer_area_container_contours)

    answer_area_container_contours.sort(key=lambda x: cv.arcLength(x, True), reverse=True)

    answer_area_container_contours = answer_area_container_contours[:num_of_answer_area_containers]

    for rect in answer_area_container_contours:
        cv.drawContours(image, [rect], -1, (0, 0, 255), 4)  # 绿色，线宽为2

    return answer_area_container_contours


def intercepting_the_answer(project_config, index_of_sheets, answer_area_container_contours, image,
                            processed_images_path):
    answer_areas = project_config['answerAreas']
    question_number_arr = []
    for answer_area in answer_areas:
        index_of_sheets_of_answer_area = int(answer_area['indexOfSheets'])
        index_of_answer_area_containers = int(answer_area['indexOfAnswerAreaContainers'])
        if index_of_sheets_of_answer_area != index_of_sheets:
            continue
        answer_area_container_contour = answer_area_container_contours[index_of_answer_area_containers]

        answer_area_container_contour_x, answer_area_container_contour_y, answer_area_container_contour_width, answer_area_container_contour_height = cv.boundingRect(
            answer_area_container_contour)

        for answer in answer_area['answers']:
            question_number = answer['questionNumber']

            x = answer_area_container_contour_x + answer_area_container_contour_width * answer['relativeLeftTopX']
            y = answer_area_container_contour_y + answer_area_container_contour_height * answer['relativeLeftTopY']
            w = (answer['relativeRightBottomX'] - answer[
                'relativeLeftTopX']) * answer_area_container_contour_width
            h = (answer['relativeRightBottomY'] - answer[
                'relativeLeftTopY']) * answer_area_container_contour_height

            answer_contour = np.array([
                [[x, y]],  # 左上角
                [[x + w, y]],  # 右上角
                [[x + w, y + h]],  # 右下角
                [[x, y + h]]  # 左下角
            ], dtype=np.int32)
            cv.drawContours(image, [answer_contour], -1, (255, 0, 0), 4)
            # 截取轮廓区域

            cropped_image = image[int(y):int(y + h), int(x):int(x + w)]
            # 保存截图到本地
            # 假设 processed_images_path 和 question_number 已经定义
            output_directory = f'{processed_images_path}'
            output_file = f'{output_directory}/{question_number}.png'

            # 检查目录是否存在，如果不存在则创建
            if not os.path.exists(output_directory):
                os.makedirs(output_directory)
            cv.imwrite(output_file, cropped_image)

            question_number_arr.append(question_number)

    print(question_number_arr)


def main():
    project_config_path = sys.argv[1]
    index_of_sheets = int(sys.argv[2])
    image_path = sys.argv[3]
    processed_images_path = sys.argv[4]

    # 加载图像，并转换为灰度图和二值图
    image = cv.imread(image_path)
    gray = cv.cvtColor(image, cv.COLOR_BGR2GRAY)
    _, thresh = cv.threshold(gray, 150, 255, cv.THRESH_BINARY)

    project_config = read_project_config(project_config_path)
    num_of_answer_area_containers = project_config['sheets'][index_of_sheets]['numOfAnswerAreaContainers']

    answer_area_container_contours = get_answer_area_container_contours(thresh, num_of_answer_area_containers, image)
    answer_area_container_contours.sort(key=lambda x: x[0][0][0])

    intercepting_the_answer(project_config, index_of_sheets, answer_area_container_contours, image,
                            processed_images_path)


def show_image(image):
    # 创建一个可调整大小的窗口
    cv.namedWindow('Contours', cv.WINDOW_NORMAL)

    # 设置窗口的大小
    cv.resizeWindow('Contours', 1200, 1600)  # 设置窗口大小为 600x600 像素

    # 显示图像
    cv.imshow('Contours', image)
    cv.waitKey(0)
    cv.destroyAllWindows()


if __name__ == '__main__':
    main()
