import java.util.*;
import cs1.Keyboard;

public class Chess {
    //board encoded as columns by rows (x by y)
    //to faciliate easy transfer between Chess coordinates and board
    //(white left rook is (0, 0))
    private Piece[][] board;

    //last move: from and to
    private int[][] lastMove;
    
    //in reversible algebraic notation (for simplicity)
    private String history;
    
    //pieces taken
    private List<Piece> blackPiecesTaken;
    private List<Piece> whitePiecesTaken;

    //keep track if continue playing
    private boolean continuePlaying;

    //~~~~~Constructor
    public Chess() {
	board = new Piece[8][8];
	populateBoard();

        lastMove = new int[2][2];
        history = "";
        
        blackPiecesTaken = new ArrayList<Piece>();
        whitePiecesTaken = new ArrayList<Piece>();
        
        continuePlaying = true;
    }

    //~~~~~Initializing board
    //Loops through the board/ [][] and populates
    private void populateBoard() {
	for (int x = 0; x < 8; x++) {
	    for (int y = 0; y < 8; y++) {
		setBoardPiece(x, y);
	    }
	}
    }
    // create 1 piece on board
    private void setBoardPiece(int x, int y) {
        if (y > 1 && y < 6)
            return;
        
	Piece p;
        
	if (y == 0 || y == 7) {
	    // white = true, black = false
	    boolean color = y == 0;
            
	    if (x == 0 || x == 7)
		p = new Rook(color);
	    else if (x == 1 || x == 6)
		p = new Knight(color);
	    else if (x == 2 || x == 5)
		p = new Bishop(color);
	    else if (x == 3)
		p = new Queen(color);
	    else // x == 4
		p = new King(color);
            
	} else { //y == 1 or 6
            boolean color = y == 1;
            p = new Pawn(color);
        }
        
        board[x][y] = p;
    }

    //~~~~~Play
    public void play() {
        //keep changing players until checkmated/draw/resign
        //also announce when player is checked (and checkmated)
        printBoard();
        int counter = 1;
        for (boolean color = true; continuePlaying; color = !color) {
            if (color) {
                history += "\n" + counter + ". ";
                counter++;
            }
            
            System.out.println(Utils.colorToString(color) + "'s turn:");
            turn(color);
            System.out.println();

            printBoard();
            
            boolean check = inCheck(!color);
            boolean noLegalMoves = noLegalMoves(!color);
            if (check) {
                System.out.println("In check!");
                if (!noLegalMoves)
                    history += "+";
            }
            if (noLegalMoves) {
                if (check)
                    System.out.println("Checkmated!");
                else
                    System.out.println("Stalemated!");
                continuePlaying = false;
            }

            history += " ";
        }

        System.out.println(history);
    }
    //~~~~~Turn
    //complete a turn for a player
    public void turn(boolean color) {
        for (;;) {
            //get input from user
            System.out.print("Enter command or coordinate of piece:\t");
            String input = Keyboard.readString();
        
            //check if valid coordinate or command
            if (!Utils.validCoordinate(input)) {
                if (!Utils.validCommand(input)) {
                    System.out.println("Unrecognized command or coordinate of piece");
                } else {
                    if (!doCommand(input)) {
                        continuePlaying = false;
                        break;
                    }
                }
                continue;
            }
            int[] from = Utils.coordToInts(input);

            //check there is own piece at coord
            Piece p = board[from[0]][from[1]];
            if (p == null || p.isWhite() != color) {
                System.out.println("Invalid piece at coordinate");
                continue;
            }
            System.out.print(p + " at " + input.substring(0, 1) + input.substring(1, 2));
            
            //get input again for move
            System.out.print("\nEnter move:\t");
            String move = Keyboard.readString();

            //check if valid coordinate
            if (!Utils.validCoordinate(move)) {
                System.out.println("Invalid coordinates of move");
                continue;
            }
            int[] to = Utils.coordToInts(move);
            
            //does move if legal, otherwise repeat
            if (turnMove(from, to, p))
                break;
            System.out.println("Invalid move");
        }
    }
    //does the move for a turn; true if successful
    private boolean turnMove(int[] from, int[] to, Piece pFrom) {
        if (!isPosMove(from, to))
            return false;
        
        if (isSpecialMove(from, to)) {
            if (!isLegalSpecialMove(from, to))
                return false;
            if (!doSpecialMove(from, to))  //failed doing move
                return false;
        } else if (isLegalMove(from, to)) {
            addHistory(from, to);
            checkAddPiecesTaken(to);
            move(from, to);
        } else {
            return false;
        }
        
        updateLastMove(from, to);
        pFrom.moved();
        return true;
    }
    
    //~~~~~Is- and Do- possible, legal, special moves
    /*
      Possible moves vs Legal moves vs Special moves:
      Possible moves only get the movements and check the boundaries
      Legal moves are possible moves but have also checked that they don't leave the king in check
      Special moves are exceptions to normal playing (ie. castling, en passant, and pawn promotion)
    */
    public boolean isPosMove(int[] from, int[] to) {
        return Utils.contains(posMoves(from[0], from[1]), to);
    }
    /*
      assume possible move
      legal if not checked when moved there
      returns true if legal
    */
    public boolean isLegalMove(int[] from, int[] to) {
	//do move (if attack, then keep track of attacked piece)
	Piece killed = board[to[0]][to[1]];
        boolean attack = killed != null;
        
        boolean legal = simulateLegalMove(from, to);
        
	if (attack)
	    board[to[0]][to[1]] = killed;
        
	return legal;
    }
    //simulate the move and return if it's legal or not (destroys whatever at to)
    private boolean simulateLegalMove(int[] from, int[] to) {
        Piece pieceFrom = board[from[0]][from[1]];
        
        move(from, to);

	//if own king not in check -> legal
        boolean legal = !inCheck(pieceFrom.isWhite());

	//undo move
	move(to, from);

        return legal;
    }
    public void move(int[] from, int[] to) {
        if (Utils.equals(from, to))
            return; //don't change anything
	board[to[0]][to[1]] = board[from[0]][from[1]];
	board[from[0]][from[1]] = null;
    }
    public boolean isSpecialMove(int[] from, int[] to) {
        Piece p = board[from[0]][from[1]];
        //pawn promotion
        if (p instanceof Pawn && p.isMoved() && isPosMove(from, to)) {
            if (to[1] == 7 || to[1] == 0)
                return true;
        }

        List<int[]> specialMoves = new ArrayList<int[]>();
        
        int xCoord = from[0];
        int yCoord = from[1];
        addSpecialMoves(xCoord, yCoord, specialMoves, false);
        addSpecialMoves(xCoord, yCoord, specialMoves, true);
        return Utils.contains(specialMoves, to);
    }
    //assume is valid special move
    //true if legal
    public boolean isLegalSpecialMove(int[] from, int[] to) {
        Piece p = board[from[0]][from[1]];
        
        if (p instanceof Pawn) {
            //pawn 2-square movement and pawn promotion, check if legal
            if (!p.isMoved() || to[1] == 7 || to[1] == 0) {
                return isLegalMove(from, to);
            } else {
                //en passant
                //check also with killing the pawn (isLegalMove doesn't do this)
                int dy = 1;
                if (p.isWhite())
                    dy = -1;
                
                Piece killed = board[to[0]][to[1] + dy];
                board[to[0]][to[1] + dy] = null;
                
                boolean legal = simulateLegalMove(from, to);
                
                board[to[0]][to[1] + dy] = killed;

                return legal;
            }
        }

        //castling
        if (p instanceof King) {
            int kingEnd = 6;
            if (to[0] < from[0])
                kingEnd = 2;
            
            //king can't be in check for any square while moving to end spot (including beginning)
            //have start be less than end
            int start = from[0];
            int end = kingEnd;
            if (end < start) {
                int temp = start;
                start = end;
                end = temp;
            }
            for (int atX = start; atX <= end; atX += 1) {
                //do move, check, and undo
                int[] toSpot = {atX, from[1]};
                boolean legal = simulateLegalMove(from, toSpot);
                if (!legal)
                    return false;
            }
        }

        return true;
    }
    //assume legal special move
    //true if successful
    public boolean doSpecialMove(int[] from, int[] to) {
        Piece p = board[from[0]][from[1]];

        if (p instanceof Pawn) {
            if (!p.isMoved()) {
                //pawn 2-square movement
                addHistory(from, to);
            } else if (to[1] == 7 || to[1] == 0){
                //keep temporary string for history (before promotion)
                String temp = historyString(from, to);
                
                //pawn promotion
                if (!promotePawn(from))
                    return false;
                Piece promoted = board[from[0]][from[1]];
                
                //for pawn promotion, put = and the new symbol after
                history += temp + "=" + promoted.toString().toUpperCase();

                checkAddPiecesTaken(to);
            } else {
                //en passant
                //also kill piece
                history += Utils.coordToString(from) + "x" + Utils.coordToString(to);
                
                int dy = 1;
                if (p.isWhite())
                    dy = -1;

                int[] killed = {to[0], to[1] + dy};
                checkAddPiecesTaken(killed);
                board[to[0]][to[1] + dy] = null;
            }
            move(from, to);
        }
        
        //castling (to is coordinate of rook)
        if (p instanceof King) {
            int kingEnd = 6;
            int dx = -1;
            if (to[0] < from[0]) {  //king moves left
                kingEnd = 2;
                dx = 1;
                history += "O-O-O";
            } else {
                history += "O-O";
            }
            int[] end = {kingEnd, from[1]};
            
            //move king
            move(from, end);

            //get rook end position
            int[] toRook = {kingEnd + dx, from[1]};
            
            //move rook
            Piece rook = board[to[0]][to[1]];
            rook.moved();
            move(to, toRook);
        }

        return true;
    }
    //assume valid pawn coordinate
    //true if successful
    private boolean promotePawn(int[] coord) {
        String s = "Promote pawn!\n";
        s += "Enter piece name you want to promote to ('q', 'r', 'b', or 'n'):";
        System.out.println(s);

        String name = Keyboard.readString().toLowerCase();

        Piece pawn = board[coord[0]][coord[1]];
        boolean color = pawn.isWhite();
        Piece promoted;
        
        switch (name) {
        case "q": promoted = new Queen(color); break;
        case "r": promoted = new Rook(color); break;
        case "b": promoted = new Bishop(color); break;
        case "n": promoted = new Knight(color); break;
        default: System.out.println("Unrecognized name of piece\n"); return false;
        }

        board[coord[0]][coord[1]] = promoted;
        
        return true;
    }

    //~~~~~inCheck
    public boolean inCheck(boolean color) {
        //get coordinate of king
        int[] kingCoord = new int[2];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece p = board[x][y];
                if (p == null || p.isWhite() != color)
                    continue;
                if (p instanceof King) {
                    kingCoord[0] = x;
                    kingCoord[1] = y;
                    break;
                }
            }
        }

        //go through all pieces of enemy player
	//if they can attack friendly king -> in check
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece p = board[x][y];
                if (p == null || p.isWhite() == color)
                    continue;
                if (Utils.contains(posMoves(x, y, true), (kingCoord)))
                    return true;
            }
        }
        
	return false;
    }

    //~~~~~noLegalMoves
    //if checked and noLegalMoves -> checkmated
    //otherwise stalemate
    public boolean noLegalMoves(boolean color) {
        //go through all of own pieces
        //if any has legal moves, return false
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                Piece p = board[x][y];
                if (p == null || p.isWhite() != color)
                    continue;
                if (legalMoves(x, y).size() > 0)
                    return false;
            }
        }

        return true;
    }

    //~~~~~Legal moves
    //returns all legal moves at coordinate
    public List<int[]> legalMoves(int xCoord, int yCoord) {
        List<int[]> posMoves = posMoves(xCoord, yCoord);
        return selectLegalMoves(xCoord, yCoord, posMoves);
    }
    //assume posMoves is valid
    public List<int[]> selectLegalMoves(int xCoord, int yCoord, List<int[]> posMoves) {
	List<int[]> legalMoves = new ArrayList<int[]>();
        int[] from = {xCoord, yCoord};
        
        //go through all possible moves and add if they are legal
        for (int[] to : posMoves) {
            if (isSpecialMove(from, to)) {
                if (isLegalSpecialMove(from, to))
                    legalMoves.add(to);
            }
            else if (isLegalMove(from, to))
                legalMoves.add(to);
        }

        return legalMoves;
    }

    //~~~~~Possible moves and adding special moves
    //get all possible moves (attack and movement)
    public List<int[]> posMoves(int xCoord, int yCoord) {
        List<int[]> posMoves = posMoves(xCoord, yCoord, true);
        posMoves.addAll(posMoves(xCoord, yCoord, false));
        return posMoves;
    }
    /*
      get possible move from coordinate (piece) (either movement or attack)
      for movement: only include when moving to empty space
      for attack: only include when hit piece
      returns list of absolute coordinates
    */
    public List<int[]> posMoves(int xCoord, int yCoord, boolean attack) {
	Piece p = board[xCoord][yCoord];
	Movement m = p.getMovements();
        if (attack && p.getMovements() != p.getAttacks())  //movements and attacks don't point to same
            m = p.getAttacks();
        
        List<int[]> posMoves = new ArrayList<int[]>();

        //go through horiz/vert/diags by using differences
        //keep going w/ differences until piece or border reached
        List<int[]> differences = new ArrayList<int[]>();
        //add horizVert differences
	if (m.isHorizVert()) {
            int[][] differenceHV = { {-1, 0}, {1, 0},   //horiz
                                     {0, -1}, {0, 1} }; //vert
            differences.addAll(Arrays.asList(differenceHV));
	}
	//add diags differences
	if (m.isDiags()) {
            int[][] differenceD = { {-1, -1}, {-1, 1},
                                    {1, -1}, {1, 1} };
            differences.addAll(Arrays.asList(differenceD));
	}
        //add all possible movements for differences
        for (int[] diffxy : differences)
            posMoves(xCoord, yCoord, posMoves, diffxy[0], diffxy[1], attack);

        //add other movements
        for (int[] move : m.getOtherMov()) {
            int atX = xCoord + move[0];
            int atY = yCoord + move[1];
            if (!p.isWhite()) {  //for black, consider movements flipped
                atX = xCoord - move[0];
                atY = yCoord - move[1];
            }
            checkAddPosMoves(xCoord, yCoord, posMoves, atX, atY, attack); //keep checking no matter return boolean
        }

        //add special moves
        addSpecialMoves(xCoord, yCoord, posMoves, attack);
        
	return posMoves;
    }
    //add possible movements given a dx and dy (for horiz/vert/diags)
    public void posMoves(int xCoord, int yCoord, List<int[]> posMoves, int dx, int dy, boolean attack) {
	int atX = xCoord;
	int atY = yCoord;
	for (;;) {
	    atX += dx;
	    atY += dy;
	    if (!checkAddPosMoves(xCoord, yCoord, posMoves, atX, atY, attack)) //false, don't check anymore
                break;
	}
    }
    //try to add possible move given coordinates.
    //return false if out of border or there was piece there
    public boolean checkAddPosMoves(int xCoord, int yCoord, List<int[]> posMoves, int x, int y, boolean attack) {
        if (x < 0 || x > 7 || y < 0 || y > 7)
            return false;
        
        Piece to = board[x][y];
        if (to != null) { // piece there
            if (attack) {
                Piece from = board[xCoord][yCoord];
                if (to.isWhite() != from.isWhite()) { //different colors
                    int[] toCoord = {x, y};
                    posMoves.add(toCoord);
                }
            }
            return false;
        }
        if (!attack) {
            int[] toCoord = {x, y};
            posMoves.add(toCoord);
        }
        return true;
    }

    public void addSpecialMoves(int xCoord, int yCoord, List<int[]> posMoves, boolean attack) {
        Piece p = board[xCoord][yCoord];
        
        if (p instanceof Pawn) {
            //pawn 2-square movement
            if (!attack && !p.isMoved()) {
                int dy = -1;
                if (p.isWhite())
                    dy = 1;
                
                //no piece 1 square ahead
                Piece at = board[xCoord][yCoord + dy];
                if (at != null)
                    return;
                
                dy *= 2;
                int[] to = {xCoord, yCoord + dy};
                posMoves.add(to);
            }

            //en passant
            if (attack && p.isMoved()) {
                //check lastMove (pawn to left or right && did double-move)
                int[] lastFrom = lastMove[0];
                int[] lastTo = lastMove[1];
                Piece atLastTo = board[lastTo[0]][lastTo[1]];
                if (!(atLastTo instanceof Pawn) || lastTo[1] != yCoord || Math.abs(lastFrom[1] - lastTo[1]) != 2 || Math.abs(lastTo[0] - xCoord) != 1)
                    return;

                int dy = -1;
                if (p.isWhite())
                    dy = 1;
                
                int[] to = {lastTo[0], yCoord + dy};
                posMoves.add(to);
            }
        }
        
        //castling
	addCastling(xCoord, yCoord, posMoves, attack);
    }
    //from: coordinate of King
    //to: coordinate of Rook
    private void addCastling(int xCoord, int yCoord, List<int[]> posMoves, boolean attack) {
	Piece p = board[xCoord][yCoord];
	
	if (attack || !(p instanceof King) || p.isMoved())
	    return;

        addCastling(xCoord, yCoord, posMoves, p, 2);
        addCastling(xCoord, yCoord, posMoves, p, 6);
    }
    //with the end spot for the king, add the castling if possible
    private void addCastling(int xCoord, int yCoord, List<int[]> posMoves, Piece p, int kingEnd) {
	//all spots from king to end spot are empty (exclude king)
        if (!isEmptyBetween(kingEnd, yCoord, xCoord))
            return;

	//get rook
        int xRook = -1;
        int dx = 1;
        if (kingEnd < xCoord) {
            dx = -1;
        }
        //go from king to left or right to find rook
        for (int atX = xCoord; atX > -1 && atX < 8; atX += dx) {
            Piece rook = board[atX][yCoord];
            if (rook == null || !(rook instanceof Rook))
                continue;
            xRook = atX;
            break;
        }
        if (xRook == -1) //not found
            return;
        
	Piece rook = board[xRook][yCoord];
	if (rook.isMoved())
	    return;
        
	//all spots from rook to end spot are empty (exclude rook)
        if (!isEmptyBetween(kingEnd, yCoord, xRook))
            return;

        int[] to = {xRook, yCoord};
        posMoves.add(to);
    }
    private boolean isEmptyBetween(int xStart, int yCoord, int xEnd) {
        //include xStart, exclude xEnd
        //have xStart be less than xEnd
        if (xEnd < xStart) {
            int temp = xStart;
            xStart = xEnd + 1;
            xEnd = temp + 1;
        }
        for (int atX = xStart; atX < xEnd; atX++) {
	    Piece at = board[atX][yCoord];
	    if (at != null)
		return false;
	}

        return true;
    }

    //~~~~~lastMove, history, piecesTaken
    public void updateLastMove(int[] from, int[] to){
	int[][] move = {from, to};
        lastMove = move;
    }
    public void addHistory(int[] from, int[] to) {
        history += historyString(from, to);
    }
    public String historyString(int[] from, int[] to) {
        Piece p = board[from[0]][from[1]];
        Piece pTo = board[to[0]][to[1]];

        //not pawn, then put capital letter
        String s = restrictiveName(p);
        s += Utils.coordToString(from);

        //if movement, then put "-" otherwise "x" and letter
        if (pTo == null) {
            s += "-";
        } else {
            s += "x";
            s += restrictiveName(pTo);
        }

        s += Utils.coordToString(to);
        
        return s;
    }
    private String restrictiveName(Piece p) {
        if (!(p instanceof Pawn))
            return p.toString().toUpperCase();
        return "";
    }

    public void checkAddPiecesTaken(int[] coord) {
        Piece p = board[coord[0]][coord[1]];
        if (p == null)
            return;
        
        if (!p.isWhite())
            blackPiecesTaken.add(p);
        else
            whitePiecesTaken.add(p);
    }

    //~~~~~Comamnds
    //assume valid command
    //return if continue playing
    public boolean doCommand(String command) {
        switch (command.toLowerCase()) {
        case "history": case "record": case "r": System.out.println(history); break;
        case "pieces": case "p": Utils.printPieces(blackPiecesTaken, whitePiecesTaken); break;
            
        case "resign": return !Utils.confirmResign();
        case "draw": return !Utils.confirmDraw();
            
        case "help" : case "h": case "?": Utils.printHelp(); break;
        case "instructions": case "i": Utils.printInstructions(); break;
            
        case "quit": case "q": case "exit": case "e": return !Utils.confirmQuit();
        }
        
        return true;
    }

    //~~~~~Print Board
    public void printBoard() {
        Utils.printBoard(board, 3, 5);
    }
}
