package searchclient;

import java.util.Comparator;

import javax.naming.InitialContext;

import java.util.*; 

class Triple<T, U, V extends Comparable<V>> implements Comparable< Triple<T, U, V>>{

    private T first;
    private U second;
    private V third;

    public Triple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T getFirst() { return first; }
    public U getSecond() { return second; }
    public V getThird() { return third; }
    public void setThird(V value) {this.third = value;}


    //@Override
    //public int compareTo(V e) {
    //    return getThird().compareTo(e);
    //}
   
    public int compareTo(Triple<T, U, V> other)
    {
        return getThird().compareTo(other.getThird());
    }
    
    public String toString(){
        return "("+getFirst().toString()+", "+ getSecond().toString() +", "+getThird().toString()+")";
    }

}

class Pair<T, U> {

    private final T first;
    private final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() { return first; }
    public U getSecond() { return second; }
}

public abstract class Heuristic
        implements Comparator<State>
{   
    private ArrayList<Triple<Integer, Integer, Character> > goal_coord = new ArrayList<Triple<Integer, Integer, Character> >();
    private int nb_goals;
    private ArrayList<  Pair<Character, ArrayList<ArrayList<Integer>> > > distances = new ArrayList<Pair<Character, ArrayList<ArrayList<Integer>> > >();

    public Heuristic(State initialState)
    {
        // Here's a chance to pre-process the static parts of the level.

        //retrieve coordinates and label of goals & nb of goals
        getGoalsData(goal_coord);
        nb_goals = goal_coord.size();

        for(int goal = 0 ; goal < nb_goals;goal++){

            // OPTION1:    from each goal build a distance map based on neighbour propagation  ------- O(N^2) worst case we explore every cell 4 times
            ArrayList<ArrayList<Integer>> distance = getdistances(goal_coord.get(goal), initialState);

            // OPTION2:    from each goal build a distance map based Disjkstra algorithm
            //ArrayList<ArrayList<Integer>> distance = getdistances_dijkstra(goal_coord.get(goal), initialState);
            
            //save the goal char and its corresponding distance map
            Pair<Character, ArrayList<ArrayList<Integer>> > pair = new Pair<Character, ArrayList<ArrayList<Integer>> >(goal_coord.get(goal).getThird(), distance);
            distances.add(pair);
        }

    }

    /**
     * builds goal_coord that contains the coordinate of the goal and its label
     * @param goal_coord container for goals data
     */
    public void  getGoalsData(ArrayList<Triple<Integer, Integer, Character> > goal_coord){
        //iterate through map and check if goal--> retrieve data
        for(int i=0;i< State.goals.length;i++){
            for(int j=0;j<State.goals[i].length;j++){
                char c = State.goals[i][j];
                if(('0' <= c && c <= '9') || ('A' <= c && c <= 'Z'))
                {
                    Triple<Integer, Integer, Character> triplet = new Triple<Integer, Integer, Character>(Integer.valueOf(i), Integer.valueOf(j), Character.valueOf(c));
                    goal_coord.add(triplet);
                }
            }
        }
 
    }

    public ArrayList<ArrayList<Integer>> getdistances_dijkstra(Triple<Integer, Integer, Character> goal_coord, State initialState ){
        ArrayList<ArrayList<Integer>> distance = init_distances(goal_coord);
        
        //holds coordinates and distances ---
        PriorityQueue<Triple<Integer, Integer, Integer>> queue = new PriorityQueue<Triple<Integer, Integer, Integer>>();
        boolean[][] visited = new boolean[distance.size()][distance.get(0).size()];
        for(int i =0; i<visited.length;i++){
            for(int j=0; j<visited[0].length;j++){
                visited[i][j]=false;
            }
        }
        //add starting point (goal)
        queue.add( new Triple<Integer, Integer, Integer> (goal_coord.getFirst(),goal_coord.getSecond(), distance.get(goal_coord.getFirst()).get(goal_coord.getSecond())));

        while(!queue.isEmpty()){
            //System.out.println(queue);

            Triple<Integer, Integer, Integer> cell = queue.poll();
            
            visited[cell.getFirst()][cell.getSecond()]=true;

            //get list of neighbors
            ArrayList<Triple<Integer, Integer, Integer>> neighbours = getValid_neighbours(cell, distance);

            for(Triple<Integer, Integer, Integer> n: neighbours){
                //if it was visited()
                if(visited[n.getFirst()][n.getSecond()] == true) continue;
                //update distance
                if( n.getThird()> cell.getThird()+1){
                    n.setThird(cell.getThird()+1);
                    distance.get(n.getFirst()).set(n.getSecond(), cell.getThird()+1);

                }
                queue.add(n);
            }

        }
        //For debugging
        //System.out.println("GOAL: "+ goal_coord.getThird());
        //System.out.println("----------------------------------------------------------------------------------");
        //for(int i =0; i<distance.size();i++){
        //    for(int j=0; j<distance.get(0).size();j++){
        //        System.out.print(distance.get(i).get(j)+" ");
        //    }
        //    System.out.println();
        //}
        //System.out.println("----------------------------------------------------------------------------------");

        return distance;
    }

    // get non wall neighbours
    private ArrayList<Triple<Integer, Integer, Integer>> getValid_neighbours(Triple<Integer, Integer, Integer> cell, ArrayList<ArrayList<Integer>> distance){

        ArrayList<Triple<Integer, Integer, Integer>> neighbours = new ArrayList<Triple<Integer, Integer, Integer>>();
        //north
        if( isValid(cell.getFirst()-1,cell.getSecond()) &&  distance.get(cell.getFirst()-1).get(cell.getSecond())  != Integer.MAX_VALUE-1){
            neighbours.add( new Triple<Integer, Integer, Integer> (cell.getFirst()-1, cell.getSecond(), distance.get(cell.getFirst()-1).get(cell.getSecond()))) ;
        }
        //East
        if( isValid(cell.getFirst(),cell.getSecond()+1) && distance.get(cell.getFirst()).get(cell.getSecond()+1)  != Integer.MAX_VALUE-1){
            neighbours.add( new Triple<Integer, Integer, Integer> (cell.getFirst(), cell.getSecond()+1, distance.get(cell.getFirst()).get(cell.getSecond()+1))) ;
        }
        //South
        if( isValid(cell.getFirst()+1,cell.getSecond()) &&  distance.get(cell.getFirst()+1).get(cell.getSecond())  != Integer.MAX_VALUE-1){
            neighbours.add( new Triple<Integer, Integer, Integer> (cell.getFirst()+1, cell.getSecond(), distance.get(cell.getFirst()+1).get(cell.getSecond()))) ;
        }
        //West
        if( isValid(cell.getFirst(),cell.getSecond()-1) && distance.get(cell.getFirst()).get(cell.getSecond()-1)  != Integer.MAX_VALUE-1){
            neighbours.add( new Triple<Integer, Integer, Integer> (cell.getFirst(), cell.getSecond()-1, distance.get(cell.getFirst()).get(cell.getSecond()-1))) ;
        }
        return neighbours;
    }

    /**
     * 
     * @param goal_coord goal data
     * @param initialState init sate
     * @return a 2d array that represents data from goals to the rest of the cells based on neighbor propagation
     */
    public ArrayList<ArrayList<Integer>> getdistances(Triple<Integer, Integer, Character> goal_coord, State initialState ){
        ArrayList<ArrayList<Integer>> distance = init_distances(goal_coord);

        boolean[][] visited = new boolean[distance.size()][distance.get(0).size()];
        for(int i =0; i<visited.length;i++){
            for(int j=0; j<visited[0].length;j++){
                visited[i][j]=false;
            }
        }
        //visited[goal_coord.getFirst()][goal_coord.getSecond()]=true;
        traverse(goal_coord.getFirst(), goal_coord.getSecond(), distance, initialState, visited);
        
        //For debugging
        //System.out.println("GOAL: "+ goal_coord.getThird());
        //System.out.println("----------------------------------------------------------------------------------");
        //for(int i =0; i<distance.size();i++){
        //    for(int j=0; j<distance.get(0).size();j++){
        //        System.out.print(distance.get(i).get(j)+" ");
        //    }
        //    System.out.println();
        //}
        //System.out.println("----------------------------------------------------------------------------------");
       
        return distance;
    }

    private ArrayList<ArrayList<Integer>> init_distances(Triple<Integer, Integer, Character> goal_coord){
        ArrayList<ArrayList<Integer>> distance = new ArrayList<ArrayList<Integer>>(State.walls.length);

        //init distances;
        for(int i=0 ; i< State.walls.length;i++){
            ArrayList<Integer> tmp = new ArrayList<Integer>(State.walls[0].length);
            for(int j=0 ; j< State.walls[0].length;j++){
                if( State.walls[i][j]==true){
                    //walls
                    tmp.add(Integer.MAX_VALUE-1);
                }else if(goal_coord.getFirst() == i && goal_coord.getSecond()==j){
                    //starting point
                    tmp.add(0);
                }else{
                    //unexplored
                    tmp.add(999);
                }
            }
            distance.add(tmp);
        }
    
        return  distance;
    }


    private boolean traverse(int i, int j, ArrayList<ArrayList<Integer>> distance, State initialState, boolean[][] visited) {

        //if edge
        if(!isValid(i, j)) return false;
        //if wall
        if (distance.get(i).get(j) == Integer.MAX_VALUE-1) {
            return false;
        }
        //calculate new min distance (either current one or neighbor distance+1)
        int min = Math.min(distance.get(i).get(j)  ,  min_neighbours(i,j, distance, initialState)+1  ) ;
        
        if(visited[i][j]==true){
            int old = distance.get(i).get(j);
            //if it changed traverse again to update the distances
            if(old>min){
                distance.get(i).set(j, min);
                 // North
                traverse(i - 1, j, distance, initialState, visited);
                // East
                traverse(i, j + 1, distance, initialState , visited);
                // South
                traverse(i + 1, j, distance, initialState, visited);
                // West
                traverse(i, j - 1, distance, initialState, visited);
            }
            return false;
        }else{
            //either self or min neighbor +1 
            distance.get(i).set(j, min);
            visited[i][j] =true;
            // North
            traverse(i - 1, j, distance, initialState, visited);
            // East
            traverse(i, j + 1, distance, initialState , visited);
            // South
            traverse(i + 1, j, distance, initialState, visited);
            // West
            traverse(i, j - 1, distance, initialState, visited);
        }
        
        return true;
    }

    //return the min distance of the 4 direct neighbours
    private int min_neighbours(int i , int j ,ArrayList<ArrayList<Integer>> distance , State state){
        int min = Integer.MAX_VALUE -1;
        
        //north
        if(isValid(i-1,j)){
            if(distance.get(i-1).get(j)< min) min = distance.get(i-1).get(j);
        } 
        //East
        if (isValid(i, j+1)){
            if(distance.get(i).get(j+1)< min) min = distance.get(i).get(j+1);
        } 
        //South
        if (isValid(i+1, j)){
            if(distance.get(i+1).get(j)< min) min = distance.get(i+1).get(j);
        } 
        //West
        if (isValid(i, j-1)){
            if(distance.get(i).get(j-1)< min) min = distance.get(i).get(j-1);
        } 
        //return min neighbour distance
        return min;
    }

    private boolean isValid(int i, int j){
        //We always have a a border of walls
        if(i>= State.walls.length-1 || i<1 || j>= State.walls[0].length-1 || j<1){
            return false;
        }
        return true;
    }


    // we pair each goal to a closest box and calculate the total distances of those min distances + the distance to the closest agent.
    public int h(State n)
    {   
        int total =0;

        ArrayList<Triple<Character, Integer, Integer>> boxes = new ArrayList<Triple<Character, Integer, Integer>>();
        //find the location and label of all boxes and store them
        for(int i =0; i< n.boxes.length;i++){
            for(int j =0; j< n.boxes[0].length;j++){
                if ('A' <= n.boxes[i][j] && n.boxes[i][j] <= 'Z')
                {   
                    //char box = n.boxes[i][j];
                    Triple<Character, Integer, Integer> box   =  new Triple<Character, Integer, Integer>(n.boxes[i][j], i , j);
                    //the box will be paired to a goal
                    boxes.add(box);
                }
            }
        }

        int pairs = 0;
        //for every goal find the closest box
        for(int goal_index=0; goal_index< nb_goals;goal_index++){
            if(nb_goals == pairs) return total;
            
            int min = Integer.MAX_VALUE;
            int min_index =-1;
            for(int box_index =0 ; box_index< boxes.size();box_index++){
                //if the box matches the goal
                if(boxes.get(box_index).getFirst() == distances.get(goal_index).getFirst()){
                    //check if it is a new min distance (distance from goal to that box)

                    //calculate the distance from the goal to the closest valid box + diatance of the agent to that box
                    int distance_goal_box = distances.get(goal_index).getSecond().get(boxes.get(box_index).getSecond()).get(boxes.get(box_index).getThird());
                    //we assume we have 1 agent (agent 0)
                    //calculate the manhattan distance from the agent to the selected box
                    int distance_agent_box = Math.abs(n.agentRows[0] - boxes.get(box_index).getSecond()) + Math.abs(n.agentCols[0] - boxes.get(box_index).getThird());

                    int curr_dis =  distance_goal_box + distance_agent_box;
                    if( curr_dis < min)
                    {
                        min = curr_dis;
                        min_index = box_index;
                    }
                }
            }
            total += min;
            boxes.remove(min_index);
            pairs++;
        }
        //System.out.println(total);
        return total;
    }



    public abstract int f(State n);

    @Override
    public int compare(State n1, State n2)
    {
        return this.f(n1) - this.f(n2);
    }
}

class HeuristicAStar
        extends Heuristic
{
    public HeuristicAStar(State initialState)
    {
        super(initialState);
    }

    @Override
    public int f(State n)
    {
        return n.g() + this.h(n);
    }

    @Override
    public String toString()
    {
        return "A* evaluation";
    }
}

class HeuristicWeightedAStar
        extends Heuristic
{
    private int w;

    public HeuristicWeightedAStar(State initialState, int w)
    {
        super(initialState);
        this.w = w;
    }

    @Override
    public int f(State n)
    {
        return n.g() + this.w * this.h(n);
    }

    @Override
    public String toString()
    {
        return String.format("WA*(%d) evaluation", this.w);
    }
}

class HeuristicGreedy
        extends Heuristic
{
    public HeuristicGreedy(State initialState)
    {
        super(initialState);
    }

    @Override
    public int f(State n)
    {
        return this.h(n);
    }

    @Override
    public String toString()
    {
        return "greedy evaluation";
    }
}
