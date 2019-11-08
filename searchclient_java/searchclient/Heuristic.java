package searchclient;

import java.util.Comparator;

import javax.naming.InitialContext;

import java.util.*; 

class Triple<T, U, V> {

    private final T first;
    private final U second;
    private final V third;

    public Triple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public T getFirst() { return first; }
    public U getSecond() { return second; }
    public V getThird() { return third; }
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
    ArrayList<Triple<Integer, Integer, Character> > goal_coord = new ArrayList<Triple<Integer, Integer, Character> >();
    int nb_goals;
    ArrayList<  Pair<Character, ArrayList<ArrayList<Integer>> > > distances = new ArrayList<Pair<Character, ArrayList<ArrayList<Integer>> > >();
    public Heuristic(State initialState)
    {
        // Here's a chance to pre-process the static parts of the level.
        
        //retrive the coordinates and number of goals cells
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
        nb_goals = goal_coord.size();

        //run disjkstra on every goa to every other cell and save distances
        for(int goal = 0 ; goal < nb_goals;goal++){
            ArrayList<ArrayList<Integer>> distance = getdistances(goal_coord.get(goal), initialState);
            //save the goal char and its corresponding distance map
            Pair<Character, ArrayList<ArrayList<Integer>> > pair = new Pair<Character, ArrayList<ArrayList<Integer>> >(goal_coord.get(goal).getThird(), distance);
            distances.add(pair);

        }
        

    }

    public ArrayList<ArrayList<Integer>> getdistances(Triple<Integer, Integer, Character> goal_coord, State initialState ){
        //run disjktra from goal_coord to rest of cells.
        ArrayList<ArrayList<Integer>> distance = new ArrayList<ArrayList<Integer>>(State.walls.length);
        for(int x=0; x< State.walls.length;x++){
            distance.add(new ArrayList<Integer>());
        }
        //init distances;
        for(int i=0 ; i< State.walls.length;i++){
            for(int j=0 ; j< State.walls[0].length;j++){
                ArrayList<Integer> tmp = new ArrayList<Integer>(State.walls[0].length);
                for(int x=0; x< State.walls[0].length;x++){
                    tmp.add(Integer.MAX_VALUE-1);
                }
                if( State.walls[i][j]==true){
                    //walls
                    tmp.set(j,Integer.MAX_VALUE-1);
                    distance.set(i, tmp);
                }else if(goal_coord.getFirst() == i && goal_coord.getSecond()==j){
                    //starting point
                    tmp.set(j,0);
                    distance.set(i, tmp);
                }else{
                    //unexplored
                    tmp.set(j,999);
                    distance.set(i, tmp);
                }
            }
        }

        traverse(goal_coord.getFirst(), goal_coord.getSecond(), distance, initialState);

        return distance;
    }

    private boolean traverse(int i, int j, ArrayList<ArrayList<Integer>> distance, State initialState) {
        //if edge
        if(!isValid(i, j, initialState)) return false;
        //if wall
        if (distance.get(i).get(j) == Integer.MAX_VALUE-1) {
            return false;
        }

        //either self or min neighbor +1 
        distance.get(i).set(j,   Math.min(distance.get(i).get(j)  ,  min_neighbours(i,j, distance, initialState)+1  )   );

        // North
        traverse(i - 1, j, distance, initialState);
        // East
        traverse(i, j + 1, distance, initialState);
        // South
        traverse(i + 1, j, distance, initialState);
        // West
        traverse(i, j - 1, distance, initialState);

        return false;
    }

    //return the min distance of the 4 direct neighbours
    private int min_neighbours(int i , int j ,ArrayList<ArrayList<Integer>> distance , State state){
        int min = Integer.MAX_VALUE-1;
        //north
        if(isValid(i-1,j,state)) min = distance.get(i-1).get(j);
        //East
        if (isValid(i, j+1, state)){
            if(distance.get(i).get(j+1)< min) min = distance.get(i).get(j+1);
        } 
        //South
        if (isValid(i+1, j, state)){
            if(distance.get(i+1).get(j)< min) min = distance.get(i+1).get(j);
        } 
        //West
        if (isValid(i, j-1, state)){
            if(distance.get(i).get(j-1)< min) min = distance.get(i).get(j-1);
        } 
        //return min neighbour distance
        return min;
    }

    private boolean isValid(int i, int j, State initialState){
        if(i>= State.walls.length || i<0 || j>= State.walls[0].length || j<0){
            return false;
        }
        return true;
    }


    public int h(State n)
    {   
        //sum of the distances from every box to the closest  goal?
        int sum=0;
        for(int i =0; i< n.boxes.length;i++){
            for(int j =0; j< n.boxes[0].length;j++){
                if ('A' <= n.boxes[i][j] && n.boxes[i][j] <= 'Z')
                {
                    // for every box, calculate distance to corresponding goal
                    // find goal
                    for(int z=0; z< distances.size();z++){
                        if(n.boxes[i][j]== distances.get(z).getFirst()){
                            sum += distances.get(z).getSecond().get(i).get(j);
                        }
                    }
                }
            }
        }
        return sum;
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
