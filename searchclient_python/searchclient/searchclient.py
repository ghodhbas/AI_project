import argparse
import io
import sys
import time

import memory
from color import Color
from state import State
from frontier import FrontierBFS, FrontierDFS, FrontierBestFirst
from heuristic import HeuristicAStar, HeuristicWeightedAStar, HeuristicGreedy

class SearchClient:
    @staticmethod
    def parse_level(server_messages) -> 'State':
        # We can assume that the level file is conforming to specification, since the server verifies this.
        # Read domain.
        server_messages.readline() # #domain
        server_messages.readline() # hospital
        
        # Read Level name.
        server_messages.readline() # #levelname
        server_messages.readline() # <name>
        
        # Read colors.
        server_messages.readline() # #colors
        agent_colors = [None for _ in range(10)]
        box_colors = [None for _ in range(26)]
        line = server_messages.readline()
        while not line.startswith('#'):
            split = line.split(':')
            color = Color.from_string(split[0].strip())
            entities = [e.strip() for e in split[1].split(',')]
            for e in entities:
                if '0' <= e <= '9':
                    agent_colors[ord(e) - ord('0')] = color
                elif 'A' <= e <= 'Z':
                    box_colors[ord(e) - ord('A')] = color
            line = server_messages.readline()
        
        # Read initial state.
        # line is currently "#initial".
        num_agents = 0
        agent_rows = [None for _ in range(10)]
        agent_cols = [None for _ in range(10)]
        walls = [[False for _ in range(130)] for _ in range(130)]
        boxes = [['' for _ in range(130)] for _ in range(130)]
        line = server_messages.readline()
        row = 0
        while not line.startswith('#'):
            for col, c in enumerate(line):
                if '0' <= c <= '9':
                    agent_rows[ord(c) - ord('0')] = row
                    agent_cols[ord(c) - ord('0')] = col
                    num_agents += 1
                elif 'A' <= c <= 'Z':
                    boxes[row][col] = c
                elif c == '+':
                    walls[row][col] = True
            
            row += 1
            line = server_messages.readline()
        del agent_rows[num_agents:]
        del agent_rows[num_agents:]
        
        # Read goal state.
        # line is currently "#goal".
        goals = [['' for _ in range(130)] for _ in range(130)]
        line = server_messages.readline()
        row = 0
        while not line.startswith('#'):
            for col, c in enumerate(line):
                if '0' <= c <= '9' or 'A' <= c <= 'Z':
                    goals[row][col] = c
            
            row += 1
            line = server_messages.readline()
        
        # End.
        # line is currently "#end".
        
        return State(agent_rows, agent_cols, agent_colors, walls, boxes, box_colors, goals)
    
    @staticmethod
    def search(initial_state: 'State', frontier: 'Frontier') -> '[[Action, ...], ...]':
        '''
        Implements the Graph-Search algorithm from R&N figure 3.7.
        '''
        
        start_time = time.perf_counter()
        iterations = 0
        
        print('Starting {}.'.format(frontier.get_name()), file=sys.stderr, flush=True)
        
        frontier.add(initial_state)
        explored = set()
        
        while True:
            if iterations == 1000:
                SearchClient.print_search_status(start_time, explored, frontier)
                iterations = 0
            
            if memory.get_usage() > memory.max_usage:
                SearchClient.print_search_status(start_time, explored, frontier)
                print('Maximum memory usage exceeded.', file=sys.stderr, flush=True)
                return None
            
            if frontier.is_empty():
                SearchClient.print_search_status(start_time, explored, frontier)
                return None
            
            leaf_state = frontier.pop()
            
            if leaf_state.is_goal_state():
                SearchClient.print_search_status(start_time, explored, frontier)
                return leaf_state.extract_plan()
            
            explored.add(leaf_state)
            for state in leaf_state.get_expanded_states():
                if state not in explored and not frontier.contains(state):
                    frontier.add(state)
            
            iterations += 1
    
    @staticmethod
    def print_search_status(start_time: 'int', explored: '{State, ...}', frontier: 'Frontier') -> None:
        status_template = '#Explored: {:8,}, #Frontier: {:8,}, #Generated: {:8,}, Time: {:3.3f} s\n[Alloc: {:4.2f} MB, MaxAlloc: {:4.2f} MB]'
        elapsed_time = time.perf_counter() - start_time
        print(status_template.format(len(explored), frontier.size(), len(explored) + frontier.size(), elapsed_time, memory.get_usage(), memory.max_usage), file=sys.stderr, flush=True)

    @staticmethod
    def main(args) -> None:
        # Use stderr to print to the console.
        print('SearchClient initializing. I am sending this using the error output stream.', file=sys.stderr, flush=True)
        
        # Send client name to server.
        sys.stdout.reconfigure(encoding='ASCII')
        print('SearchClient', flush=True)
        
        # We can also print comments to stdout by prefixing with a #.
        print('#This is a comment.', flush=True)
        
        # Parse the level.
        server_messages = sys.stdin
        server_messages.reconfigure(encoding='ASCII')
        initial_state = SearchClient.parse_level(server_messages)
        
        # Select search strategy.
        frontier = None
        if args.bfs:
            frontier = FrontierBFS()
        elif args.dfs:
            frontier = FrontierDFS()
        elif args.astar:
            frontier = FrontierBestFirst(HeuristicAStar(initial_state))
        elif args.wastar is not False:
            frontier = FrontierBestFirst(HeuristicWeightedAStar(initial_state, args.wastar))
        elif args.greedy:
            frontier = FrontierBestFirst(HeuristicGreedy(initial_state))
        else:
            # Default to BFS search.
            frontier = FrontierBFS()
            print('Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.', file=sys.stderr, flush=True)
        
        # Search for a plan.
        plan = SearchClient.search(initial_state, frontier)
        
        # Print plan to server.
        if plan is None:
            print('Unable to solve level.', file=sys.stderr, flush=True)
            sys.exit(0)
        else:
            print('Found solution of length {}.'.format(len(plan)), file=sys.stderr, flush=True)
            
            for joint_action in plan:
                print(";".join(a.name_ for a in joint_action), flush=True)
                # We must read the server's response to not fill up the stdin buffer and block the server.
                response = server_messages.readline()

if __name__ == '__main__':
    # Program arguments.
    parser = argparse.ArgumentParser(description='Simple client based on state-space graph search.')
    parser.add_argument('--max-memory', metavar='<MB>', type=float, default=2048.0, help='The maximum memory usage allowed in MB (soft limit, default 2048).')
    
    strategy_group = parser.add_mutually_exclusive_group()
    strategy_group.add_argument('-bfs', action='store_true', dest='bfs', help='Use the BFS strategy.')
    strategy_group.add_argument('-dfs', action='store_true', dest='dfs', help='Use the DFS strategy.')
    strategy_group.add_argument('-astar', action='store_true', dest='astar', help='Use the A* strategy.')
    strategy_group.add_argument('-wastar', action='store', dest='wastar', nargs='?', type=int, default=False, const=5, help='Use the WA* strategy.')
    strategy_group.add_argument('-greedy', action='store_true', dest='greedy', help='Use the Greedy strategy.')
    
    args = parser.parse_args()
    
    # Set max memory usage allowed (soft limit).
    memory.max_usage = args.max_memory
    
    # Run client.
    SearchClient.main(args)
