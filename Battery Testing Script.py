#Import statements
import datetime
#This will be the power testing script for testing if the GPS can turn on and off

#First open the logfile
logfile = open("powerlogfile.txt", "w")

#Declaring variables, currently in testing mode with flat values
amps = 0.5
watts = 1
volts = 5
trial = 1

logline=""
powerline = ""
#Declaring time variables.
curtime = 0


#Basic For loop structure
for gpsStat in ["On","Off"]:
    #Control the total amount of time. Currently in seconds
    #Minutes - 1, 2, 5, 10, 15
    for endtime in [60, 120, 300, 600, 900]:
        for screenstatus in ["On", "Off"]:
            trial = 1
            while trial <= 20:
                while curtime <= endtime:
                    logline = ""
                    #Let's simulate some alterations, shall we?
                    #In the production code - this should be replaced with reading from the power meter
                    if curtime % 2 == 0:
                        amps = amps + 1.6
                        volts = volts + 5.1
                    else:
                        amps = amps / 2.0
                        volts = volts / 2
                    watts = watts + 1
                    powerline = str("%.2f" % watts) + "W, " + str( "%.2f" % amps) + "A, " + str("%.2f" % volts) + "V"
                    #Get the current datestamp
                    now = datetime.datetime.now()
                   
                    #Let's convert the data to a loggable form
                    logline = gpsStat + " | " + str(curtime)+ "s" + " | " + screenstatus + " | " + "#" + str(trial) + " | " + powerline + " | " + str(now)
                    
                    curtime = curtime + 1
                    
                    print(logline)
                    #Write to the logfile
                    logfile.write(logline + "\n")
                curtime = 0
                trial = trial + 1
                
           
            
logfile.close()
