#A short script to run statistics on our timing data
import statistics
import glob

#A function to both print to console and write to file
def print_write(lineToWrite):
    print(lineToWrite)
    outfile.write(lineToWrite + "\n")

#So that I'm not typing out the stats repeatedly
def get_stats(listToStats):
    print_write("Mean: " + str("{:.4f}".format(statistics.mean(listToStats) * 0.000000001)) + "s")
    print_write("Std: " + str("{:.4f}".format(statistics.stdev(listToStats) * 0.000000001)) + "s")
    print_write("Min: " + str("{:.4f}".format(min(listToStats)* 0.000000001)) + "s")
    print_write("Max: " + str("{:.4f}".format(max(listToStats)* 0.000000001)) + "s")
    
outfilepath = """Data Logs/Timing/TimingStats.txt"""
globfilepath = """Data Logs/Timing/*.txt"""
listfiles = glob.glob(globfilepath)
outfile = open(outfilepath, "w")
#Text lines will combine all of the lines - filedivided will divide by file
filedivided = []
textlines = []
#Since for verification sake: we'll include stats for verification
verificationlines = []
#Now to open all of the files
for filepath in listfiles:
    if(filepath == """Data Logs/Timing\TimingStats.txt"""):
        continue
    file = open(filepath, "r")
    fileinput = file.readlines()
    textlines.extend(fileinput)
    filedivided.append(fileinput)
    if(not "TimingLog-Desk.txt" in filepath and not "TimingLog-Desk2.txt" in filepath):
        print("H!")
        verificationlines.extend(fileinput)
    file.close()

#Get all of the first fix lines
firstfix = [w for w in textlines if w[:8] == "FIRSTFIX"]
timetaken = [int(w.split("|")[2]) for w in firstfix]
#Get the overall first fix statistics - print them oout and write them to a file

print_write("Overall Statistics : ")
get_stats(timetaken)

#Verifiation statistics - showcase the stats of first fix versus onlocationchanged
print_write("\nVerification Statistics : ")
firstfixv = [w for w in verificationlines if w[:8] == "FIRSTFIX"]
firstlocv = [w for w in verificationlines if w[:8] == "FIRSTLOC"]
#Remove an outlier point that I don't know how it got into the data
firstlocv.remove("FIRSTLOC|554422057693|325243958558|325243ms|Mon Nov 11 14:01:59 EST 2019\n")
timetakenff = [int(w.split("|")[2]) for w in firstfixv]
timetakenfl = [int(w.split("|")[2]) for w in firstlocv]
#Print out the statistics
print_write("\nFirst Fix - GPS Listener : ")
get_stats(timetakenff)
print_write("\nFirst Loc - Location Manager On Location Changed : ")
get_stats(timetakenfl)

#Now to break down the individual file statistics
print_write("\nFile by file stats: ")
firstfixbyfile = [[x for x in w if x[:8] == "FIRSTFIX"] for w in filedivided]
timetakenbyfile = [[int(x.split("|")[2])for x in w] for w in firstfixbyfile]

#Now loop
for filenums in timetakenbyfile:
    listindex = timetakenbyfile.index(filenums)
    print_write("\n" + listfiles[listindex][len("""Data Logs/Timing/"""):])
    get_stats(filenums)

outfile.close()



