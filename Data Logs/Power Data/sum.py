def main():
    f = open("wattage_ON_OFF_2.txt", "r")
    read = f.readlines()
    sum = 0
    for i in read:
        if not i.__contains__("#"):
            sum = sum + float(i)
        else:
            print(str(sum))
            sum = 0
    print(str(sum))
    f.close()

if __name__ == "__main__":
    main()