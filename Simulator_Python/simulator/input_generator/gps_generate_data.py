import json
import math
import sys
import random
from argparse import ArgumentParser

doc_dict = {}
point = {}
file_car = open('car.txt', 'w')
file_motor = open('motorcycle.txt', 'w')
file_walk = open('walk.txt', 'w')
file_bicycle = open('bicycle.txt', 'w')
node = random.randint(5, 10)


def car():
    header = []
    n = 'car'
    max_speed = random.randint(40, 60)
    min_speed = random.randint(20, 40)
    avg_speed = (min_speed + max_speed) / 2
    d1 = random.randint(100, 1000)
    x =0
    y =0
    new_d = 0
    new_d = new_d + d1
    header.append(n)
    header.append(avg_speed)
    header.append(min_speed)
    header.append(max_speed)
    header.append(new_d)

    file_car.write(str(header))
    for i in range(node):
        angle = random.randint(130,230)
        point = []
        #d1 = random.randint(100, 300)
        new_d = 0
        new_d = new_d + d1
        a = math.cos(math.radians(angle)) * d1
        b = math.sin(math.radians(angle)) * d1
        r = random.randint(-1,1)
        u = random.randint(-1, 1)
        a = round(a)
        b = round(b)

        if r == 1 & u == 1:
            x = x + a
            y = y + b
        elif r == 1 & u == -1:
            x = x + a
            y = y - b
        elif r == 1 & u ==0:
            x = x + a
            y = y+0
        elif r == -1 & u == 1:
            x = x - a
            y = y + b
        elif r == -1 & u == -1:
            x = x - a
            y = y - b
        elif r ==-1 &  u == 0:
            x = x - a
            y = y+0
        elif r == 0 & u ==1:
            x = x + 0
            y = y + b
        elif r ==0 & u ==-1:
            x = x + 0
            y = y - b
        elif r == 0 & u == 0:
            x=x+0
            y=y+0

        # x = x + a
        # y = y + b
        print(r)
        print(u)
        print(angle)
        point.append((x,y))
        point.append((angle,u,r))
        file_car.write('\n')
        file_car.write(str(point))


def motorcycle():
    header =[]
    n = 'motorcycle'
    max_speed = random.randint(50, 80)
    min_speed = random.randint(30, 40)
    avg_speed = (min_speed + max_speed) / 2
    d1 = random.randint(100, 1000)
    x =0
    y =0
    new_d = 0
    new_d = new_d + d1
    header.append(n)
    header.append(avg_speed)
    header.append(min_speed)
    header.append(max_speed)
    header.append(new_d)

    file_motor.write(str(header))
    for i in range(node):
        angle = random.randint(130, 230)
        point = []
        #d1 = random.randint(100, 300)
        new_d = 0
        new_d = new_d + d1
        a = math.cos(math.radians(angle)) * d1
        b = math.sin(math.radians(angle)) * d1
        r = random.randint(-1,1)
        u = random.randint(-1, 1)
        a = round(a)
        b = round(b)
        if r == 1 & u == 1:
            x = x + a
            y = y + b
        elif r == 1 & u == -1:
            x = x + a
            y = y - b
        elif r == 1 & u ==0:
            x = x + a
            y = y+0
        elif r == -1 & u == 1:
            x = x - a
            y = y + b
        elif r == -1 & u == -1:
            x = x - a
            y = y - b
        elif r ==-1 &  u == 0:
            x = x - a
            y = y+0
        elif r == 0 & u ==1:
            x = x + 0
            y = y + b
        elif r ==0 & u ==-1:
            x = x + 0
            y = y - b
        elif r == 0 & u == 0:
            x=x+0
            y=y+0
        print(r)
        print(u)
        print(angle)
        point.append((x,y))
        point.append((angle,u,r))
        file_motor.write('\n')
        file_motor.write(str(point))


def bicycle():
    header =[]
    n = 'bicycle'
    max_speed = random.randint(10, 15)
    min_speed = random.randint(5, 9)
    avg_speed = (min_speed + max_speed) / 2
    d1 = random.randint(100, 300)
    x =0
    y =0
    new_d = 0
    new_d = new_d + d1
    header.append(n)
    header.append(avg_speed)
    header.append(min_speed)
    header.append(max_speed)
    header.append(new_d)
    file_bicycle.write(str(header))
    for i in range(node):
        angle = random.randint(0, 360)
        point = []
        #d1 = random.randint(100, 300)
        new_d = 0
        new_d = new_d + d1
        a = math.cos(math.radians(angle)) * d1
        b = math.sin(math.radians(angle)) * d1
        r = random.randint(-1,1)
        u = random.randint(-1, 1)
        a = round(a)
        b = round(b)
        if r == 1 & u == 1:
            x = x + a
            y = y + b
        elif r == 1 & u == -1:
            x = x + a
            y = y - b
        elif r == 1 & u ==0:
            x = x + a
            y = y+0
        elif r == -1 & u == 1:
            x = x - a
            y = y + b
        elif r == -1 & u == -1:
            x = x - a
            y = y - b
        elif r ==-1 &  u == 0:
            x = x - a
            y = y+0
        elif r == 0 & u ==1:
            x = x + 0
            y = y + b
        elif r ==0 & u ==-1:
            x = x + 0
            y = y - b
        elif r == 0 & u == 0:
            x=x+0
            y=y+0

        print(r)
        print(u)
        print(angle)
        point.append((x,y))
        point.append((angle,u,r))
        file_bicycle.write('\n')
        file_bicycle.write(str(point))


def walk():
    header =[]
    n = 'walk'
    max_speed = random.randint(4, 5)
    min_speed = random.randint(2, 3)
    avg_speed = (min_speed + max_speed) / 2
    d1 = random.randint(0, 300)
    x =0
    y =0
    new_d = 0
    new_d = new_d + d1
    header.append(n)
    header.append(avg_speed)
    header.append(min_speed)
    header.append(max_speed)
    header.append(new_d)
    file_walk.write(str(header))
    for i in range(node):
        angle = random.randint(0, 360)
        point = []
        #d1 = random.randint(100, 300)
        new_d = 0
        #new_d = new_d + d1
        a = math.cos(math.radians(angle)) * d1
        b = math.sin(math.radians(angle)) * d1
        r = random.randint(-1,1)
        u = random.randint(-1, 1)
        a = round(a)
        b = round(b)
        if r == 1 & u == 1:
            x = x + a
            y = y + b
        elif r == 1 & u == -1:
            x = x + a
            y = y - b
        elif r == 1 & u ==0:
            x = x + a
            y = y+0
        elif r == -1 & u == 1:
            x = x - a
            y = y + b
        elif r == -1 & u == -1:
            x = x - a
            y = y - b
        elif r ==-1 &  u == 0:
            x = x - a
            y = y+0
        elif r == 0 & u ==1:
            x = x + 0
            y = y + b
        elif r ==0 & u ==-1:
            x = x + 0
            y = y - b
        elif r == 0 & u == 0:
            x=x+0
            y=y+0
        print(r)
        print(u)
        # x = x + a
        # y = y + b
        print(angle)
        point.append((x,y))
        point.append((angle,u,r))
        file_walk.write('\n')
        file_walk.write(str(point))


car()
motorcycle()
walk()
bicycle()


