/**
 * Name: Trevor Stewart
 * Assignment: CYK Algorithm
 */

import java.io.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;//preserves the insertion order unlike HashMap
import java.util.Scanner;

public class CYK {

    public static LinkedHashMap<String, ArrayList<String>> GrammarTable = new LinkedHashMap<>();
    public static ArrayList<String> QueryStrings = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        //The input file must have the correct formatting as this does not check for that
        File inputFile = new File(args[0]); //java CYK.java input.txt
        //File inputFile = new File("input.txt");

        //Initialize the output file and its method of writing
        BufferedWriter outputWriter = new BufferedWriter(new FileWriter("output.txt"));

        //Initialize the scanner and see how many operations the program must do
        Scanner scan = new Scanner(inputFile);
        int operations = scan.nextInt();

            for (int i = 0; i < operations; i++) {
                //Store grammar and list of strings for this iteration
                fillGrammarTable(scan);
                fillQueryStrings(scan);
                String startSymbol = getStartSymbol();

            //CYK for each string in the list of QueryStrings
            for (int strNum = 0; strNum < QueryStrings.size(); strNum++) {
                refactoredCYK(strNum,startSymbol);
            }

            //Append to the output file (each sting is now tagged with result)
            appendOutput(outputWriter);

            //Clear the storage for the next iteration
            GrammarTable.clear();
            QueryStrings.clear();
        }

        //End of program, close IOs
        outputWriter.close();
        scan.close();

    }

    /**
     * This method will check whether or not a string (given by an index of QueryStrings)
     * appears in the language supplied by the current grammar within the GrammarTable.
     * <p></p>
     * Note: method will alter the query string by concatenating the result to the string.
     *
     * @param  strAtIndex  the index of the current query string being evaluated from the ArrayList QueryStrings.
     * @param  startSymbol the left hand side non-terminal that is the starting point of the grammar.
     */
    public static void refactoredCYK(int strAtIndex, String startSymbol){
        //Grab the string to be checked
        String str = QueryStrings.get(strAtIndex);

        //Initialize the dynamic array
        int tableSize = str.length(); // [ rows ] [ columns ]
        String[][] jaggedArray = initializeJaggedArray(tableSize);

        //Fill out the jaggedArray
        for(int row = 0; row < tableSize; row++) {

            //The length of the sub-string is determined by the current row
            int subStrLength = row + 1;

            for (int column = 0; column < tableSize; column++) {

                //The list of LHS non-Terminals to add to the current square
                ArrayList<String> nonTermsThatContain = new ArrayList<>();

                //Depending on which row we are looking to fill out, we will have one of two cases...

                // Case 1.)
                //SubString is a single char, simple case of checking if the productions contain the char
                if(row == 0){
                    String charAt = String.valueOf(str.charAt(column));
                    nonTermsThatContain = computeFirstRowCase(charAt);
                }

                // Case 2.)
                //SubString has a length greater than 1, so each split of the subString must be evaluated
                else{
                    //Check if the current subString is within the bounds of the table
                    int endIndex = column + subStrLength;

                    if(endIndex >= str.length() + 1) {
                        continue;//Continue to the next row if it is not...
                    }

                    //Actually create the sub-string
                    String subStr = str.substring(column,endIndex);

                    //Generate all the possible productions by the splits of the sub-string
                    ArrayList<String> splitProductions = generateSplitProductions(row,column,subStr,jaggedArray);

                    //Check each LHS to see if it contains any of the productions generated by the splits
                    nonTermsThatContain = computeNthRowCase(splitProductions);
                }

                //Fill in the current cell of the table, and clear the list for the next iteration
                jaggedArray[row][column] = String.join(",", nonTermsThatContain);
                nonTermsThatContain.clear();
            }
        }

        //Tag the current string with its result
        String result = getCYKResult(startSymbol,jaggedArray[tableSize-1][0]);
        QueryStrings.set(strAtIndex,str + result);

        //printStringTable(jaggedArray);
    }

    /**
     * Helper method for generating all the possible productions for each split pair of the sub-string
     * @param i the starting row
     * @param j the starting column
     * @param subStr the current sub-string
     * @param jaggedArray the table being filled out
     * @return returns a list of possible productions
     */
    public static ArrayList<String> generateSplitProductions(int i, int j, String subStr, String[][] jaggedArray){
        int row = i, column = j;

        //This method splits the current subString into each of possible splits,
        // and returns a list of their combined productions

        //This is the list of productions to be returned
        ArrayList<String> list = new ArrayList<>();

        //Need look at all the possible splits
        for(int splitSpot = 1; splitSpot < subStr.length(); splitSpot++) {
            //Split the subStr at the current splitting point...
            // (the Strings aren't needed but helped to visualize)
            String splitLeft = subStr.substring(0,splitSpot);
            String splitRight = subStr.substring(splitSpot);

            //These should be the locations of the table we would need to look at
            // for the current split iteration, there's a pattern to it.
            int iLeft = splitSpot - 1, jLeft = column;//splitLeft cell coordinates
            int iRight = i - 1, jRight = j + 1;//splitRight cell coordinates

            //Load up the symbols that were in each split's table cell
            String[] leftSplitSymbols = jaggedArray[iLeft][jLeft].split(",");
            String[] rightSplitSymbols = jaggedArray[iRight][jRight].split(",");

            //Combine each symbol from leftSplit with each symbol of rightSplit
            for (String leftSplitSymbol : leftSplitSymbols) {
                for (String rightSplitSymbol : rightSplitSymbols) {
                    //Combine the symbols into a new possible production
                    String combinedProduction = leftSplitSymbol.concat(rightSplitSymbol);

                    //Avoid adding duplicates!!
                    if(!list.contains(combinedProduction))
                        list.add(combinedProduction);
                }
            }
            i--;j++;//For the next pair of splits' locations
        }

        return list;
    }

    /**
     * The tougher case for CYK, needs to compare an entire list of possible productions
     * to each of the keys in the keySet of GrammarTable to see if any of the left hand side
     * non-terminals have one of the productions.
     * @param list the ArrayList of possible productions
     * @return returns an ArrayList of the left hand side non-terminals that contain
     * a production for the sub-string
     */
    public static ArrayList<String> computeNthRowCase(ArrayList<String> list){

        //Need to check if any of the left side non-terminals have a rule for the production
        ArrayList<String> nonTermContains = new ArrayList<>();

        //Search keys to see if their list contains any of the productions
        for (String nonTerminal:GrammarTable.keySet()) {
            for (String production: list) {
                //Add the nonTerminal if it has a transition for the char
                if(GrammarTable.get(nonTerminal).contains(production)){
                    nonTermContains.add(nonTerminal);
                    break;//Just need the first occurrence
                }
            }
        }

        return nonTermContains;
    }

    /**
     * The simple case for CYK, checks each left hand side non-terminal to see
     * if it contains a production for the single character sub-string.
     * @param charAt the sub-string of size one
     * @return returns an ArrayList of the left hand side non-terminals that contain
     * a production for the sub-string
     */
    public static ArrayList<String> computeFirstRowCase(String charAt){

        //List for keeping track which LHS nonTerminals can go to charAt
        ArrayList<String> nonTermContains = new ArrayList<>();

        //Search keys to see if their list contains the char
        for (String nonTerminal:GrammarTable.keySet()) {

            //Add the nonTerminal if it has a transition for the char
            if (GrammarTable.get(nonTerminal).contains(charAt)) {
                nonTermContains.add(nonTerminal);
            }
        }

        return nonTermContains;
    }

    /**
     * (NOT USED) This was just my first iteration of the CYK, it is roughly the same just sloppier.
     * It still has the print statements for testing and such...
     * @param num index of the current string
     * @param startSymbol the start symbol of the grammar
     */
    public static void computeCYK(int num, String startSymbol){
        //Grab the string to be checked
        String str = QueryStrings.get(num);
        System.out.println("String to be checked:  " + str);

        //Initialize the dynamic array
        int tableSize = str.length(); // [ rows ] [ columns ]
        String[][] jaggedArray = initializeJaggedArray(tableSize);

        //Fill out the jaggedArray
        for(int row = 0; row < tableSize; row++){
            for(int column = 0; column < tableSize; column++){

                //Based on the length of the substring,


                //TODO: Refactor into its own method
                if(row == 0){//First row only, only looking at a single char of str
                    String charAt = String.valueOf(str.charAt(column));

                    //System.out.println("Looking at: " + charAt); //---Testing
                    //List for keeping track which nonTerminals can go to charAt
                    ArrayList<String> nonTermContains = new ArrayList<>();

                    //Search keys to see if their list contains the char
                    for (String nonTerminal:GrammarTable.keySet()) {

                        //Add the nonTerminal if it has a transition for the char
                        if(GrammarTable.get(nonTerminal).contains(charAt)){
                            //System.out.println(nonTerminal + " contains " + charAt); //---Testing
                            nonTermContains.add(nonTerminal);
                        }
                        //else System.out.println("nothin happend..."); //---Testing
                    }
                    //Add the list in the form of a joined comma separated string
                    jaggedArray[row][column] = String.join(",", nonTermContains);
                }

                //Every other case:
                else{
                    int subStrLength = row + 1;
                    //System.out.println("SubStr has a length of: " + subStrLength);//----Testing

                    int endIndex = column + subStrLength;

                    if(endIndex >= str.length() + 1){
                        continue;
                    }

                    //Create the sub string
                    String subStr = str.substring(column,endIndex);
                    //System.out.println(subStr + "   EndIndex: " + endIndex);// ------------Testing

                    //To go through the splits table locations
                    int i = row;
                    int j = column;

                    System.out.println(subStr + "  [ " + i + " , " + j + " ]");

                    //For trying out each combination of their productions
                    ArrayList<String> list = new ArrayList<>();//splitProductions


                    //Need look at all the possible splits
                    for(int splitSpot = 1; splitSpot < subStrLength; splitSpot++) {
                        String splitLeft = subStr.substring(0,splitSpot);
                        int iLeft = splitSpot - 1, jLeft = column;
                        String splitRight = subStr.substring(splitSpot);
                        int iRight = i - 1, jRight = j + 1;
                        String splitInfo =  "[" + iLeft + "," + jLeft + "] " + splitLeft + " "
                                            + splitRight + " [" + iRight + "," + jRight + "]";
                        System.out.println(subStr + " --> " + splitInfo + " " +
                                            jaggedArray[iLeft][jLeft] + " " + jaggedArray[iRight][jRight]);

                        //Need to create the productions
                        String[] l = jaggedArray[iLeft][jLeft].split(",");
                        String[] r = jaggedArray[iRight][jRight].split(",");
                        //moved list from here

                        for (String leftSplitProduction : l) {
                            for (String rightSplitProduction : r) {
                                //
                                String production = leftSplitProduction.concat(rightSplitProduction);
                                System.out.println(leftSplitProduction + rightSplitProduction);//---Testing
                                //Only add the
                                if(!list.contains(production)) list.add(production);


                            }
                        }
                        i--;j++;//for the next splits locations
                    }

                    //Need to check if any of the left side non-terminals have a rule for the production
                    ArrayList<String> nonTermContains = new ArrayList<>();

                    //Search keys to see if their list contains any of the productions
                    for (String nonTerminal:GrammarTable.keySet()) {
                        for (String production: list) {
                            //Add the nonTerminal if it has a transition for the char
                            if(GrammarTable.get(nonTerminal).contains(production)){
                                System.out.println(nonTerminal + " contains " + production); //---Testing
                                nonTermContains.add(nonTerminal);
                            }
                        }

                    }
                    //Add the list in the form of a joined comma separated string
                    //TODO: the last iteration of this per substr will override everything, need ot append rather than hard set
                    jaggedArray[row][column] = String.join(",", nonTermContains);
                    list.clear();

                }
            }
        }

        //Print the jaggedArray
        printStringTable(jaggedArray);

        //Tag the current string with its result
        String result = getCYKResult(startSymbol,jaggedArray[tableSize-1][0]);
        QueryStrings.set(num,str + result);
    }

    /**
     * Helper method that checks if the start symbol is within the last cell
     *
     * @param  startSymbol  the start symbol of the grammar
     * @param  lastIndex  the bottom most cell of the table
     * @return result string to be concatenated to the query string
     */
    public static String getCYKResult(String startSymbol, String lastIndex){
        if(lastIndex.contains(startSymbol))
            return " is in the language defined by the above grammar"; //pass
        else
            return " is NOT in the language defined by the above grammar"; //fail
    }

    /**
     * Helper method for generating the jagged array to be filled out during the CYK algo.
     * @param tableSize  the number of rows that will be needed
     * @return a jagged array of size tableSize
     */
    public static String[][] initializeJaggedArray(int tableSize){
        String[][] jaggedArray = new String[tableSize][];
        int columnSize = tableSize;

        //For each row down the table, the column size is one smaller than the previous
        for (int i = 0; i < tableSize; i++) {
            jaggedArray[i] = new String[columnSize];
            columnSize--;
        }

        return jaggedArray;
    }

    /**
     * Appends the current grammar and list of query string to a file
     *
     * @param  writer  the BufferedWriter that appends to output.txt
     */
    public static void appendOutput(BufferedWriter writer) throws IOException {

        for (String nonTerminal:GrammarTable.keySet()) {
            for (String production : GrammarTable.get(nonTerminal)) {
                writer.append(nonTerminal).append(" --> ").append(production).append("\n");
            }
        }

        writer.newLine();

        for (String queryString : QueryStrings) {
            writer.append(queryString).append("\n");
        }

        writer.newLine();
    }

    /**
     * Fills out the LinkedHashMap GrammarTable
     * <p>
     * - Each left hand symbol is a key to the LinkedHashMap
     * </p>
     * - Each production after the "-->" is a string in an Arraylist tied to a key
     *
     * @param  scan  the Scanner that reads from input.txt
     */
    public static void fillGrammarTable(Scanner scan) {

        //Declare the two sides of each line of the grammar
        String nonTerminal = "", production = " ", line = null;

        //TODO: check if empty line, could be more elegant
        scan.nextLine();scan.nextLine();

        while(!(line = scan.nextLine()).isEmpty()){

            //System.out.println(line);//TODO: For Testing
            if(!line.contains("-->")) break;

            //Split the values into the non-terminal (values[0]), and its production (values[1])
            String[] values = line.split("-->");
            nonTerminal = values[0].strip();
            production = values[1].strip();

            //System.out.println(nonTerminal + "-->" + production);//TODO: For Testing

            //Just add to the already existing production list if non-terminal exists
            if(GrammarTable.containsKey(nonTerminal)){
                GrammarTable.get(nonTerminal).add(production);
            }
            //Create a new list for the non-terminal, then add the production list
            else{
                GrammarTable.put(nonTerminal, new ArrayList<String>());
                GrammarTable.get(nonTerminal).add(production);

            }
        }
        //System.out.println(GrammarTable);//TODO: For Testing
    }

    /**
     * Fills out the ArrayList QueryStrings
     * <p>
     * - The file must first specify the total number of strings
     * </p>
     * @param  scan  the Scanner that reads from input.txt
     */
    public static void fillQueryStrings(Scanner scan) {
        int numOfStrings = scan.nextInt();
        for (int i = 0; i < numOfStrings; i++) {
            QueryStrings.add(scan.next());
        }
    }

    /**
     * Helper method that just grabs the first key of GrammarTable
     */
    public static String getStartSymbol(){
        //Grab the first item in the keySet
        return GrammarTable.keySet().toArray(new String[0])[0];
    }

    /**
     * Prints out the given table out to the terminal.
     * This was just for testing...
     * @param table the table to be visualized.
     */
    public static void printStringTable(String[][] table){//Method for Trouble Shooting
        String emptyCell = "          ";//10 spaces

        for (String[] row : table) {
            for (String element : row) {
                if(element == null || element.isEmpty() || element.isBlank())
                    System.out.print(" | " + emptyCell);
                else
                    System.out.print(" | " + element.concat(emptyCell.substring(element.length())));
            }
            System.out.print(" |\n");
        }
    }
}