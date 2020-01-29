import sys, math, random, ast, numpy

#Constants
STAY_OFF = 0
TURN_ON = 1
STAY_ON = 2
TURN_OFF = 3

#These are default values. They can be set with the tags -rt, -rp, and -p. Once set they are constant for the duration of the simulation.
READY_TIME = 3 #Number of seconds needed to turn on the gps and acquire the signal.
READY_PWR = 2 #Units of power/energy used to turn the gps on and acquire the signal.
PWR_USAGE = 5 #Units of power used to update location.

DETAILED_OUTPUT = False #If true, print the locations after every iteration. Set to True with -d.
OUT_FILE = None #Specify an output file with -o. The file will be created if it does not exist. File opens with the 'w' mode.
				#Do not include the '.txt' extension. It will be added automatically.

def distance_to(pt1, pt2):
	return math.sqrt(((pt2[0] - pt1[0]) ** 2) + ((pt2[1] - pt1[1]) ** 2))
	
#Returns the horizontal or vertical component of the user's velocity vector
#uord is 1 if deltaY is > 0 and -1 if deltaY is < 0
#rorl is 1 if deltaX is > 0 and -1 if deltaX is < 0
#Multiplying by uord and rorl determines which quadrant the user's velocity vector is in.
def get_vel_comp(spd, angle, uord, rorl, horz):
	#Vertical component
	if horz == False:
		if angle == 0: #There is no vertical component
			return 0
		elif angle == 90: #Motion is entirely vertical
			return spd * uord
		else: 
			return spd * math.sin(math.radians(angle)) * uord
	else:
		if angle == 0: #Motion is entirely horizontal
			return spd * rorl
		elif angle == 90: #There is no horizontal component
			return 0
		else:
			return spd * math.cos(math.radians(angle)) * rorl

#Due to precision differences in certain calculations, it is possible for the user to pass through a point
#but miss the exact x, y values. If one value is overshot, assume the point has been reached. This is a temporary solution.		
def missed_point(ud, rl, pt, loc):
	if ud == 1:
		if rl == 1:
			if pt[0] < loc[0] or pt[1] < loc[1]:
				return True
			else:
				return False
		elif rl == -1:
			if pt[0] > loc[0] or pt[1] < loc[1]:
				return True
			else:
				return False
		elif rl == 0:
			if pt[1] < loc[1]:
				return True
			else:
				return False
	elif ud == -1:
		if rl == 1:
			if pt[0] < loc[0] or pt[1] > loc[1]:
				return True
			else:
				return False
		elif rl == -1:
			if pt[0] > loc[0] or pt[1] > loc[1]:
				return True
			else:
				return False
		elif rl == 0:
			if pt[1] > loc[1]:
				return True
			else:
				return False
	elif ud == 0:
		if rl == 1:
			if pt[0] < loc[0]:
				return True
			else:
				return False
		elif rl == -1:
			if pt[0] > loc[0]:
				return True
			else:
				return False
		else:
			return False
	else:
		print("Error: invalid up or down value.")
		quit()
		
#Use adjust to affect how far the margin of error extends
def calc_margin(spd, adjust):
	dist_until_on = READY_TIME * spd
	return (spd * adjust) + dist_until_on

class User:
	def __init__(this, start, spd, dir):
		this.loc = start #start = [x, y]
		this.vel = [spd, dir] #User's velocity. [m/s, (angle, uord, rorl)]
	
	def update_loc(this):
		s = this.vel[0] #speed
		a = this.vel[1][0] #angle
		ud = this.vel[1][1] #up or down
		rl = this.vel[1][2] #right or left
		horz_comp = get_vel_comp(s, a, ud, rl, True) #deltaX
		vert_comp = get_vel_comp(s, a, ud, rl, False) #deltaY
		this.loc[0] += horz_comp
		this.loc[1] += vert_comp
		return 1
		
	def update_vel(this, new_spd, new_dir):
		this.vel[0] = new_spd
		this.vel[1] = new_dir
		return 1
		
class GPS:
	def __init__(this, start):
		this.loc = start
		this.active = False
		this.time_until_ready = -1 #Timer for determining when the gps is ready
		
	#updates the gps's reported location and determines whether to change the status of the gps
	def update(this, user, moe, endpt): 
		if user.vel[0] == 0:
			return STAY_ON
		dist_to_end = distance_to(user.loc, endpt) #Distance between the user and endpt
		if this.active == True and this.time_until_ready < 0:
			this.loc[0] = user.loc[0]
			this.loc[1] = user.loc[1]
			if dist_to_end <= moe: 
				return STAY_ON
			else:
				time_to_endpt = dist_to_end / user.vel[0]
				#Calculate the horizontal and vertical distances from endpt to errorpt. (see below)
				horz_dist_to_error = get_vel_comp(moe, user.vel[1][0], user.vel[1][1] * -1, user.vel[1][2] * -1, True)
				vert_dist_to_error = get_vel_comp(moe, user.vel[1][0], user.vel[1][1] * -1, user.vel[1][2] * -1, False)
				#errorpt is the point where the margin of error begins
				errorpt = (endpt[0] + horz_dist_to_error, endpt[1] + vert_dist_to_error)
				time_to_errorpt = distance_to(user.loc, errorpt) / user.vel[0]
				if (READY_PWR + PWR_USAGE * (time_to_endpt - time_to_errorpt)) > (PWR_USAGE * time_to_endpt):
					return STAY_ON
				else:
					return TURN_OFF
				
		elif this.active == False:
			if dist_to_end <= moe:
				return TURN_ON 
			else:
				return STAY_OFF
				
class Scenario:
	def __init__(this, header, points):
		temp_pts = points.copy()
		num_pts = len(points) - 1
		mean_spd = header[1]
		min = header[2]
		max = header[3]
		std_dev = float((max - min) / num_pts)
		
		#Generates an approximately normal distribution of speed values.
		speeds = numpy.random.normal(mean_spd, std_dev, num_pts)
		for s in speeds:
			if s > max:
				s = max
			elif s < min:
				s = min
			s = round(s, 1)
			
		for i in range(len(speeds)):
			if len(temp_pts[i]) < 3:
				temp_pts[i].append(speeds[i])
			else: #If multiple simulations are being run, change values.
				temp_pts[i][2] = speeds[i]
			
		temp_pts[num_pts].append(0.0)
		#for i in range(len(points)):
			#print(points[i][2])
			
		this.header = header
		this.keypts = temp_pts
		this.numpts = len(this.keypts)
					
def main_loop(user, gps, scen):
	print("Scenario: " + scen.header[0])
	#Min and Max are limits. The actual min and max speeds will depend on the random generation.
	print("Speed (Avg/Min/Max): " + str(scen.header[1]) + "/" + str(scen.header[2]) + "/" + str(scen.header[3]) + " m/s")
	print("Average Distance: " + str(scen.header[4]) + " m")
	print("READY_TIME == " + str(READY_TIME))
	print("READY_PWR == " + str(READY_PWR))
	print("PWR_USAGE == " + str(PWR_USAGE))
	print("Starting simulation...")
	if OUT_FILE is not None:
		OUT_FILE.write("Scenario: " + scen.header[0] + "\n")
		OUT_FILE.write("Speed (Avg/Min/Max): " + str(scen.header[1]) + "/" + str(scen.header[2]) + "/" + str(scen.header[3]) + " m/s\n")
		OUT_FILE.write("Average Distance: " + str(scen.header[4]) + " m\n")
		OUT_FILE.write("READY_TIME == " + str(READY_TIME) + "\n")
		OUT_FILE.write("READY_PWR == " + str(READY_PWR) + "\n")
		OUT_FILE.write("PWR_USAGE == " + str(PWR_USAGE) + "\n")
		OUT_FILE.write("Starting simulation...\n")
		
	T = 0
	status = STAY_OFF
	new_status = None
	n_pt = scen.keypts[1] #next point
	n_pt_i = 1 #index of n_pt in scen.keypts
	pts = scen.numpts
	pwr = 0
	dist_to_next = distance_to(user.loc, n_pt[0])
	
	#This value is set multiple times. If you change how it is calculated, make sure to change it everywhere.
	margin_of_error = calc_margin(user.vel[0], 2)
	
	while n_pt_i < pts:
		T += 1
		gps.time_until_ready -= 1
		if gps.time_until_ready == 0:
			print("At " + str(T) + " seconds the GPS acquired a signal.")
			if OUT_FILE is not None:
				OUT_FILE.write("At " + str(T) + " seconds the GPS acquired a signal.\n")
		if user.vel[0] >= dist_to_next or missed_point(user.vel[1][1], user.vel[1][2], n_pt[0], user.loc):
			#For the sake of simplicity, if the user is going to pass through the next point,
			#assume that making the turn and adjusting speed uses the rest of the second.
			user.loc[0] = n_pt[0][0]
			user.loc[1] = n_pt[0][1]
		else:
			user.update_loc()
			
		
		#print(user.vel[0])
		
		new_status = gps.update(user, margin_of_error, n_pt[0])
		if new_status != status:
			if new_status == TURN_ON:
				print("At " + str(T) + " seconds the GPS began turning on.")
				if OUT_FILE is not None:
					OUT_FILE.write("At " + str(T) + " seconds the GPS began turning on.\n")
				gps.time_until_ready = READY_TIME
				pwr += READY_PWR
				gps.active = True
			elif new_status == TURN_OFF:
				print("At " + str(T) + " seconds the GPS turned off.")
				if OUT_FILE is not None:
					OUT_FILE.write("At " + str(T) + " seconds the GPS turned off.\n")
				pwr += PWR_USAGE
				gps.active = False
		if new_status == STAY_ON:
			pwr += PWR_USAGE
			#if margin_of_error / 2 > user.vel[0]:
				#margin_of_error /= 2
		
		status = new_status
		
		if DETAILED_OUTPUT:
			print("User: " + str(user.loc))
			print("GPS: " + str(gps.loc))
			print("Next point: " + str(n_pt[0]))
			print("----")
			if OUT_FILE is not None:
				OUT_FILE.write("User: " + str(user.loc) + "\n")
				OUT_FILE.write("GPS: " + str(gps.loc) + "\n")
				OUT_FILE.write("Next point: " + str(n_pt[0]) + "\n")
				OUT_FILE.write("----\n")
		
		if distance_to(user.loc, n_pt[0]) < 1:
			#Adjust the user's position to account for potential drifting
			user.loc[0] = n_pt[0][0] 
			user.loc[1] = n_pt[0][1]
			n_pt_i += 1 #Look at the next key point
			if n_pt_i < pts:
				user.update_vel(n_pt[2], n_pt[1]) #Change the user's speed and direction
				n_pt = scen.keypts[n_pt_i] #Load next point
				dist_to_next = distance_to(user.loc, n_pt[0]) #Calculate distance to next point
				margin_of_error = calc_margin(user.vel[0], 2) #Calculate new margin of error
	
	print("End of simulation.")
	print("Total Time: " + str(T) + " seconds.")
	print("Total Power Used: " + str(pwr) + " units.")
	if OUT_FILE is not None:
		OUT_FILE.write("End of simulation.\n")
		OUT_FILE.write("Total Time: " + str(T) + " seconds.\n")
		OUT_FILE.write("Total Power Used: " + str(pwr) + " units.\n")
		
if __name__ == "__main__":
	argv = sys.argv
	argc = len(argv)
	fname = "NOFILE" #Input filename. Required.
	oname = "NOFILE" #Output filename. Optional.
	n = 1 #Number of simulations. Set with -n. The iteration number will be appended to the output filename if given.
	
	for i in range(argc):
		if i == 0:
			continue
		if argv[i] == "-f":
			fname = argv[i+1]
			i += 1
		elif argv[i] == "-rt":
			temp = int(argv[i+1])
			i += 1
			if temp < 0:
				print("Invalid value for \'READY_TIME\'. Using default value: " + str(READY_TIME))
			else:
				READY_TIME = temp
		elif argv[i] == "-rp":
			temp = int(argv[i+1])
			i += 1
			if temp < 0:
				print("Invalid value for \'READY_PWR\'. Using default value: " + str(READY_PWR))
			else:
				READY_PWR = temp
		elif argv[i] == "-p":
			temp = int(argv[i+1])
			i += 1
			if temp <= 0:
				print("Invalid value for \'PWR_USAGE\'. Using default value: " + str(PWR_USAGE))
			else:
				PWR_USAGE = temp
		elif argv[i] == "-d":
			DETAILED_OUTPUT = True
		elif argv[i] == "-o":
			oname = argv[i+1]
			i += 1
		elif argv[i] == "-n":
			n = int(argv[i+1])
			i += 1
			if n <= 0:
				print("Invalid value for \'n\'. Using default value: 1.")
				n = 1
			
	if fname == "NOFILE":
		print("Error. No input file supplied.")
		quit()
		
	infile = open(fname)
	
	head = ast.literal_eval(infile.readline())
	pts = list(())
	line = infile.readline()
	while line != "":
		pts.append(ast.literal_eval(line))
		line = infile.readline()
		
	infile.close()
	
	for i in range(n):
		scen = Scenario(head, pts)
		user = User([0, 0], scen.keypts[0][2], scen.keypts[0][1])
		gps = GPS([0, 0])
		if oname != "NOFILE":
			if n > 1:
				OUT_FILE = open(oname + "_" + str(i) + ".txt", 'w')
			else:
				OUT_FILE = open(oname + ".txt", 'w')
		main_loop(user, gps, scen)
		if OUT_FILE is not None:
			OUT_FILE.close()