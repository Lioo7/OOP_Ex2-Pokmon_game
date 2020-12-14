package gameClient;

import Server.Game_Server_Ex2;
import api.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Ex2 implements Runnable{
    private static GameFrame gFrame;
    private static Arena arena;

    public static void main(String[] args) {
        Thread client = new Thread(new Ex2());
        client.start();
    }

    @Override
    public void run() {

        //Game initializing
        int level_number = 0; //The level of the game [0,24]
        game_service game = Game_Server_Ex2.getServer(level_number);
        System.out.println(game); //Prints the server details
        String gameGraph = game.getGraph();
        System.out.println(gameGraph); //Prints the graph details
        String pokemons = game.getPokemons();
        System.out.println(pokemons); //Prints the pokemons details

        //Pre-launch
        DWGraph_Algo graph_algo = new DWGraph_Algo();

        //Loads all the data into the graph
        graph_algo.load(gameGraph);

        //Creates a list which will contain all the pokemons in the game.
        List<CL_Pokemon> pokemonsList = Arena.json2Pokemons(pokemons);

        int src_node = 0;  // arbitrary node, you should start at one of the fruits
        game.addAgent(src_node);

        String agents = game.getAgents();

        //Creates a list which will contain all the agents in the game.
        List<CL_Agent> agentsList = Arena.getAgents(agents, graph_algo.getGraph());

         /*
        Creates a priority queue which will contain all the pokemons in the game.
        The priority queue ranks the pokemons by their values from the greater to the lesser.
         */
        PriorityQueue<CL_Pokemon> pokemonsPQ = new PriorityQueue<>(new Pokimon_Comparator());

        //Moves all the pokemons from the list to the PQ
        pokemonsPQ.addAll(pokemonsList);

        /*
        Locates all the agents in the graph,
        the first agent locates in the closest node to the pokemon with the greatest value and etc.
         */
        for (int i = 0; i < agentsList.size(); i++) {
            CL_Pokemon currPokemon = pokemonsPQ.poll();
            int pokemon_src = getPokemonNode(currPokemon, (DWGraph_DS) graph_algo.getGraph());
            //locates the current agent in the nearest node to the pokemon.
            game.addAgent(pokemon_src);
        }
        System.out.println(agents); //Prints the agents details

        /*
        -------------------------------------------------------------------------------------------------
        Game Launching
        -------------------------------------------------------------------------------------------------
         */
        try {
            init(game);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        game.startGame();
        gFrame.setTitle("OOP Ex2 " + game.toString());
        int ind=0;

        //Initialize an ArrayList that contains all the targeted pokemons.
        List<CL_Pokemon> targetedPokemons = new ArrayList<>();

        //Keep running while the game is on
        while (game.isRunning()) {
            for (CL_Agent currAgent : agentsList) {

                //Takes an agent from the agentList.
                //Checks if the agent is at a node, if it is gives him a new destination.
                if (currAgent.getNextNode() != -1) {

                    //Finds the nearest pokemon with the greatest value .
                    CL_Pokemon target = getNearestPokemon(currAgent, graph_algo, targetedPokemons, game);

                    //Finds the nearest node to the target.
                    int pokemonNode = getPokemonNode(target, (DWGraph_DS) graph_algo.getGraph());

                    //Calculates which node will be the next destination
                    int newDest = nextNode(currAgent, pokemonNode, graph_algo);

                    //Sets a new destination for the current agent.
                    game.chooseNextEdge(currAgent.getID(), newDest);

                    //Agent details
                    int agentID = currAgent.getID();
                    double agentValue = currAgent.getValue();
                    System.out.println("Agent: " + agentID + ", value: " + agentValue + " is moving to node: " + newDest);
                }

                //Moves all the agents.
                game.move();

                try {
                    if (ind % 2 == 0)
                        gFrame.repaint();
                    Thread.sleep(100);
                    ind++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String game_str = game.toString();

            System.out.println(game_str);
            System.exit(0);
        }
    }

    /*
        -------------------------------------------------------------------------------------------------
        Functions
        -------------------------------------------------------------------------------------------------
         */
    private void init(game_service game) throws FileNotFoundException {
        String graph_str = game.getGraph();
        String pokemons = game.getPokemons();
        DWGraph_Algo gameGraph = new DWGraph_Algo();
        gameGraph.load(graph_str);
        arena= new Arena();
        arena.setGraph(gameGraph.getGraph());
        arena.setPokemons(Arena.json2Pokemons(pokemons));
        gFrame = new GameFrame("OOP Ex2");
        gFrame.setSize(1000, 700);
        gFrame.update(arena);

        gFrame.show();
        String info = game.toString();
        JSONObject line;
        try {
            line = new JSONObject(info);
            JSONObject ttt = line.getJSONObject("GameServer");
            int rs = ttt.getInt("agents");
            System.out.println(info);
            System.out.println(game.getPokemons());
            ArrayList<CL_Pokemon> cl_fs = Arena.json2Pokemons(game.getPokemons());
            for (CL_Pokemon cl_f : cl_fs)
                Arena.updateEdge(cl_f, gameGraph.getGraph());
            for(int a = 0; a < rs; a++) { // fail
                int ind = a % cl_fs.size();
                CL_Pokemon c = cl_fs.get(ind);
                int nn = c.get_edge().getDest();
                if(c.getType() < 0)
                    nn = c.get_edge().getSrc();
                game.addAgent(nn);
            }
        }
        catch (JSONException e) {e.printStackTrace();}
    }

    /**
     * The function gets an agent and a graph and returns the nearest pokemon with the greatest value,
     * by compute the value/the distance.
     *
     * @param agent      the agent
     * @param graph_algo the graph
     * @return the nearest pokemon with the greatest value
     */
    private static CL_Pokemon getNearestPokemon(CL_Agent agent, DWGraph_Algo graph_algo, List<CL_Pokemon> targetedPokemons, game_service game) {
        int srcNode = agent.getSrcNode();
        String pokemons = game.getPokemons();
        CL_Pokemon result = null;
        double distance;
        double minScore = 0;

        //Creates a list which will contain all the pokemons in the game.
        List<CL_Pokemon> PokemonsList = Arena.json2Pokemons(pokemons);

        /*
        Iterates all the pokemons in the game that is not targeted yet,
        And checks which pokemon has the greatest valueForDistance.
         */
        for (CL_Pokemon currentPokemon : PokemonsList) {
            //Checks if the current pokemon is not targeted already.
            if (!targetedPokemons.contains(currentPokemon)) {
                int pokemonSrc = getPokemonNode(currentPokemon, (DWGraph_DS) graph_algo.getGraph());
                distance = graph_algo.shortestPathDist(srcNode, pokemonSrc);
                if (distance > 0) {
                    double score = getValueForDistance(distance, currentPokemon);
                    if (score > minScore) {
                        minScore = score;
                        result = currentPokemon;
                    }
                }
            }
        }

        //Marks the pokemon as targeted by adding it to the targeted list.
        targetedPokemons.add(result);

        //Returns the targeted pokemon.
        return result;
    }

    /**
     * The functions gets a distance and a pokemon and returns the quotient of the distance/the speed
     * of the pokemon.
     *
     * @param distance       the distance
     * @param currentPokemon the pokemon
     * @return the quotient of the distance/the speed of the pokemon
     */
    private static double getValueForDistance(double distance, CL_Pokemon currentPokemon) {
        return currentPokemon.getValue() / distance;
    }

    /**
     * The function gets a pokemon and a graph and returns the nearest node to the pokemon
     *
     * @param currentPokemon the pokemon
     * @param myGraph        the graph
     * @return the nearest node to the pokemon
     */
    private static int getPokemonNode(CL_Pokemon currentPokemon, DWGraph_DS myGraph) {
        /*
            Checks the direction of the edge by its type:
            If the type is positive then the pokemon goes from the lesser to the greater node,
            so takes the minimum between src and dest.
            Else the pokemon goes from the greater to the lesser node,
            so takes the maximum between src and dest.
             */
        Arena.updateEdge(currentPokemon, myGraph);
        edge_data pokemonEdge = currentPokemon.get_edge();
        int pokemonSrc;
        if (currentPokemon.getType() > 0) {
            pokemonSrc = Math.min(pokemonEdge.getSrc(), pokemonEdge.getDest());
        } else {
            pokemonSrc = Math.max(pokemonEdge.getSrc(), pokemonEdge.getDest());
        }
        return pokemonSrc;
    }

    /**
     * The function gets a pokemon and a graph and returns the next step towards that pokemon (the new dest).
     * @param agent the agent
     * @param dest the nearest node to the target pokemon
     * @param graph_algo the graph
     * @return the new destination of agent
     */
    private static int nextNode(CL_Agent agent, int dest, DWGraph_Algo graph_algo) {
        int src = agent.getSrcNode();
        return graph_algo.shortestPath(src, dest).get(1).getKey();
    }

}
