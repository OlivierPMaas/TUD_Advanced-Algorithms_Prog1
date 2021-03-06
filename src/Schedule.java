import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Schedule implements Comparable<Schedule> {
	// A linked-list is a relatively efficient representation of a schedule
	// Feel free to modify it if you feel there exists a better one
	// The main advantage is that in a search-tree there is a lot of overlap
	// between schedules, this implementation stores this overlap only once
	public Schedule previous;
	public int jobID;
	public double jobLength;
	public double jobDueTime;
	
	// tardiness can be calculated instead of memorized
	// however, we need to calculate it a lot, so we memorize it
	// if memory is an issue, however, try calculating it
	private double tardiness;

	public Schedule(){
		this.previous = null;
		this.jobID = -1;
		this.jobLength = 0;
		this.tardiness = 0;
		this.jobDueTime = 0;
	}
	
	// add an additional job to the schedule
	public Schedule(Schedule s, int jobID, double jobLength, double jobDueTime){
		this.previous = s;
		this.jobID = jobID;
		this.jobLength = jobLength;
		this.jobDueTime = jobDueTime;
		this.tardiness = Math.max(0, getTotalTime() - jobDueTime);
		
		if(previous != null) {
			this.tardiness += previous.getTardiness();
		}
	}

	// --Switched out code, as suggested by teacher's comments.
	//
	// used by the best-first search
	// currently, schedules are traversed in smallest total tardiness order
	public int compareTo(Schedule o){
		//return getTardiness() - o.getTardiness();

		// replace with the following to get a depth-first search
		return this.getDepth() - o.getDepth();
	}

	// Used by EDP
	// Gives the job with highest processing time
	public Schedule findK() {
		// base case
		if (this.previous == null) {
			return this;
		}
		// recursion
		Schedule candidate = previous.findK();
		if (this.jobLength >= candidate.jobLength) {
			return this;
		} else {
			return candidate;
		}
	}

	// Used by EDP
	public Schedule removeK() {
		int k = this.findK().jobID;
		return removeID(k);
	}

	// Used by EDP
	public Schedule removeID(int id) {
		if(this.jobID == id) {
			return this.previous;
		}
		if(this.previous == null) {
			return this;
		}
		else if(this.previous.jobID == id) {
			Schedule secondlast = this.previous.previous;
			if(secondlast != null) {
				Schedule output = new Schedule(secondlast, this.jobID, this.jobLength, this.jobDueTime);
				output.tardiness = secondlast.tardiness + Math.max(0,
						secondlast.getTotalTime() + this.jobLength - jobDueTime);
				return output;
			}
			else {
				Schedule output = new Schedule(null,this.jobID, this.jobLength,this.jobDueTime);
				output.tardiness = Math.max(0,this.jobLength - jobDueTime);
				return output;
			}
		}
		else {
			Schedule output = new Schedule(this.previous.removeID(id), this.jobID, this.jobLength, this.jobDueTime);
			output.tardiness = output.previous.getTardiness() + Math.max(0, this.getTotalTime() - jobDueTime);
			return output;
		}
	}

	public int getDepth(){
		int depth = 1;
		if(previous != null) depth += previous.getDepth();
		return depth;
	}


	// Used by EDP
	public Schedule getScheduleBetween(int id, int id2) {
		if(id < 0) {
			throw new java.lang.Error("Tried to get too large a schedule");
		}
		if(id2 >= this.jobID) {
			return this.cutOffBefore(id);
		}
		else if(this.previous != null) {
			return this.previous.getScheduleBetween(id, id2);
		}
		else {
			return null;
		}
	}

	public Schedule cutOffBefore(int id) {
		if(this.jobID < id) {
			return null;
		}
		else if(this.jobID == id) {
			Schedule cutOff = new Schedule(null, this.jobID, this.jobLength, this.jobDueTime);
			return cutOff;
		}
		// if(this.jobID > id)
		else {
			Schedule result;
			if(this.previous != null) {
				result = new Schedule(this.previous.cutOffBefore(id), this.jobID, this.jobLength, this.jobDueTime);
			}
			else {
				result = new Schedule(null, this.jobID, this.jobLength, this.jobDueTime);
			}
			return result.fixTardiness(0);
		}
	}

	public Schedule fixTardiness(int startTime) {
		if(this.previous == null) {
			this.tardiness = Math.max(0,startTime + this.jobLength - this.jobDueTime);
			return this;
		}
		else {
			this.previous = this.previous.fixTardiness(startTime);
			this.tardiness = Math.max(0, this.previous.getTardiness() + this.jobLength - this.jobDueTime);
			return this;
		}
	}

	public void printer() {
		System.out.println(this.jobID);
		if(this.previous != null) {
			this.previous.printer();
		}
	}

	public double getTotalTime(){
		double time = jobLength;
		if(previous != null) time += previous.getTotalTime();
		return time;
	}

	public double getCompletionTime(double startTime) {
		if(this.previous == null) {
			return this.jobLength + startTime;
		}
		else {
			return this.jobLength + this.previous.getCompletionTime(startTime);
		}
	}

	public int getMinJobID() {
		if(this.previous == null) {
			return this.jobID;
		}
		else {
			return this.previous.getMinJobID();
		}
	}

	public List<Integer> getJobs() {
		List<Integer> result = new ArrayList<Integer>();
		result.add(this.jobID);
		if(this.previous != null) {
			result.addAll(this.previous.getJobs());
		}
		return result;
	}

	public Schedule rescale(double K) {
		Schedule rescaledSchedule;
		if(this.previous == null) {
			rescaledSchedule = new Schedule(null, this.jobID,
					(int) Math.floor(jobLength/K), jobDueTime/K);
		}
		else {
			rescaledSchedule = new Schedule(this.previous.rescale(K),this.jobID,
					(int) Math.floor(jobLength/K), jobDueTime/K);
		}
		return rescaledSchedule.fixTardiness(0);
	}

	public double getMaxIndividualTardiness(double startTime) {
		if(this.previous == null) {
			return Math.max(0,this.getCompletionTime(startTime) - this.jobDueTime);
		}
		else {
			double previousMax = this.previous.getMaxIndividualTardiness(startTime);
			return Math.max(previousMax, Math.max(0,this.getCompletionTime(startTime)-this.jobDueTime));
		}
	}

	public ProblemInstance makeProblemInstance() {
		if(this.previous != null) {
			ProblemInstance previousInstance = this.previous.makeProblemInstance();
			double[][] previousJobs = previousInstance.getJobs();
			int n = previousInstance.getNumJobs() + 1;
			double[][] jobs;
			jobs = new double[n][2];
			for (int i = 0; i < n - 1; i++) {
				jobs[i] = previousJobs[i];
			}
			jobs[n-1][0] = this.jobLength;
			jobs[n-1][1] = this.jobDueTime;
			return new ProblemInstance(n, jobs);
		}
		double[][] jobs = new double[1][2];
		jobs[0][0] = this.jobLength;
		jobs[0][1] = this.jobDueTime;
		return new ProblemInstance(1,jobs);
	}

	public double getTardiness(){
		return tardiness;
	}
	
	public boolean containsJob(int job){
		return (jobID == job) || (previous != null && previous.containsJob(job));
	}
}
