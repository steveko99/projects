#include <cstdlib>
#include <iostream>

// num_rows and num_cols must be the same
// i.e the board must be square
#define NUM_ROWS 3
#define NUM_COLS 3

#define MAX_LINE 512

#define TIC_TAC_EMPTY_CHAR    '_'     // empty character on the board
#define TIC_TAC_USER_CHAR     'X'     // user
#define TIC_TAC_COMPUTER_CHAR 'O'     // vs. computer

#define TIC_TAC_LEVEL_1        1      // only have one difficulty level so far

class CTicTacToeBoard
{
    private:
        char m_BoardData[NUM_ROWS][NUM_COLS];

    public:
        CTicTacToeBoard();
        ~CTicTacToeBoard();

    public:
        void Draw(void);
        void Reset(void);
        bool PlotPoint(char c, int m, int n);
        bool CheckForWinner(void);

        bool IsPointTaken(int m, int n);
        bool IsUserPoint(int m, int n);
        bool IsComputerPoint(int m, int n);
};

class CTicTacToeGame
{
    private:
        int m_difficulty_level;
        CTicTacToeBoard m_Board;

    private:
        void ComputerMoveLevel1(void);
        void ComputerMoves(void);
        bool UserMoves(void);

    public:
        CTicTacToeGame();

    public:
        void Start(void);
        void SetDifficultyLevel(int l) { m_difficulty_level = l; };
};

/******************************************************************************
 *
 * Implementation of CTicTacToeBoard methods
 *
 *****************************************************************************/

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
CTicTacToeBoard::CTicTacToeBoard()
{
    Reset();
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
CTicTacToeBoard::~CTicTacToeBoard()
{
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
void
CTicTacToeBoard::Reset(void)
{
    memset(m_BoardData, TIC_TAC_EMPTY_CHAR, sizeof(m_BoardData));
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
bool
CTicTacToeBoard::IsPointTaken(int m, int n)
{
    if ( m_BoardData[m][n] == TIC_TAC_USER_CHAR ||
         m_BoardData[m][n] == TIC_TAC_COMPUTER_CHAR )
        return true;

    return false;
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
bool
CTicTacToeBoard::IsUserPoint(int m, int n)
{
    if ( m_BoardData[m][n] == TIC_TAC_USER_CHAR )
        return true;

    return false;
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
bool
CTicTacToeBoard::IsComputerPoint(int m, int n)
{
    if ( m_BoardData[m][n] == TIC_TAC_COMPUTER_CHAR )
        return true;

    return false;
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
void
CTicTacToeBoard::Draw(void)
{
    for ( int i=0; i<NUM_ROWS; i++ )
    {
        for ( int j=0; j<NUM_ROWS; j++ )
        {
            std::cout << m_BoardData[i][j];
        }
        std::cout << std::endl;
    }
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
bool
CTicTacToeBoard::PlotPoint(char c, int m, int n)
{
    if ( m < 0 || m >= NUM_ROWS )
        return false;

    if ( n < 0 || n >= NUM_COLS )
        return false;

    m_BoardData[m][n] = c;
    return true;
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
bool
CTicTacToeBoard::CheckForWinner(void)
{
    // check for 3 across
    for ( int i=0; i<NUM_ROWS; i++ )
    {
        bool bRun = true;
        for ( int j=1; j<NUM_COLS; j++ )
        {
            if ( !IsPointTaken(i,j) || m_BoardData[i][j-1] != m_BoardData[i][j] )
            {
                bRun = false;
                break;
            }
        }

        if ( bRun )
        {
            std::cout << "Winner! 3 across!" << std::endl;
            return true;
        }
    }

    // check for 3 up/down
    for ( int j=0; j<NUM_COLS; j++ )
    {
        bool bRun = true;
        for ( int i=1; i<NUM_ROWS; i++ )
        {
            if ( !IsPointTaken(i,j) || m_BoardData[i-1][j] != m_BoardData[i][j] )
            {
                bRun = false;
                break;
            }
        }

        if ( bRun )
        {
            std::cout << "Winner! 3 down!" << std::endl;
            return true;
        }
    }
 
    //
    // check for diagonal
    // this assumes the board is square, though it could be > 3 X 3
    // and if the board is not square, then the diagonal has no meaning
    //
    // todo: put an assert to be sure board is square else this could
    // index past end of array
    //
    {
        bool bRun = true;
        for ( int i=1; i<NUM_ROWS; i++ )
        {
            if ( !IsPointTaken(i,i) || m_BoardData[i-1][i-1] != m_BoardData[i][i] )
            {
                bRun = false;
                break;
            }
        }

        if ( bRun )
        {
            std::cout << "Winner! diagonal left-right!" << std::endl;
            return true;
        }
    }

    // check for right to left diagonal
    {
        bool bRun = true;
        for ( int i=0; i<NUM_ROWS-1; i++ )
        {
            int colnum = NUM_COLS - i - 1;
            if ( !IsPointTaken(i,colnum) || m_BoardData[i][colnum] != m_BoardData[i+1][colnum-1] )
            {
                bRun = false;
                break;
            }
        }

        if ( bRun )
        {
            std::cout << "Winner! diagonal right-left!" << std::endl;
            return true;
        }
    }

    // we didn't find a run
    return false;
}

/******************************************************************************
 *
 * Implementation of CTicTacToeGame methods
 *
 *****************************************************************************/

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
CTicTacToeGame::CTicTacToeGame(void)
{
    SetDifficultyLevel(TIC_TAC_LEVEL_1);
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
void
CTicTacToeGame::ComputerMoveLevel1(void)
{
    for ( int i=0; i<NUM_ROWS; i++ )
    {
        for ( int j=0; j<NUM_COLS ; j++ )
        {
            if ( ! m_Board.IsPointTaken(i,j) )
            {
                m_Board.PlotPoint(TIC_TAC_COMPUTER_CHAR, i, j);
                return;
            }
        }
    }
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
void
CTicTacToeGame::ComputerMoves(void)
{
    // level-1 algorithm, for the kids
    switch ( m_difficulty_level )
    {
        case TIC_TAC_LEVEL_1:
            ComputerMoveLevel1();
            break;

        default:
            ComputerMoveLevel1();       // only have level1 today
            break;
    }
}

//-----------------------------------------------------------------------------
// returns true if user plotted a point
// returns false if user wants to quit playing the game
//-----------------------------------------------------------------------------
bool
CTicTacToeGame::UserMoves(void)
{
    char buff[MAX_LINE];
    int rownum, colnum;

    // loop stops whenever we actually plot a point
    // routine bails if user exits with 'q' key
    while ( true )
    {
        // get the row number from the user, stop loop when it is in range
        while ( true )
        {
            std::cout << "Enter your move -- row: (1,2,3) ";
            std::cin.getline(buff, MAX_LINE);
            rownum = strtoul(buff, NULL, 10);

            if ( buff[0] == 'q' )
                return false;

            if ( rownum > 0 && rownum <= NUM_ROWS )
                break;

            std::cout << "Invalid row number - try again" << std::endl;
        }

        // get the column number from the user, stop loop when it is in range
        while ( true )
        {
            std::cout << "Enter your move -- column: (1,2,3) ";
            std::cin.getline(buff, MAX_LINE);
            colnum = strtoul(buff, NULL, 10);

            if ( buff[0] == 'q' )
                return false;

            if ( colnum > 0 && colnum <= NUM_COLS )
                break;

            std::cout << "Invalid column number - try again" << std::endl;
        }

        // we ask the user for 1-based indexes
        // now convert them to zero-based
        rownum--;
        colnum--;

        if ( m_Board.IsPointTaken(rownum, colnum) )
        {
            std::cout << "This point is already taken -- try again" << std::endl;
        }

        else
        {
            m_Board.PlotPoint(TIC_TAC_USER_CHAR, rownum, colnum);
            break;
        }
    }

    return true;
}

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
void
CTicTacToeGame::Start(void)
{
    std::cout << "Press \'q\' at any time to quit" << std::endl;
    m_Board.Draw();

    // breaks out when user presses 'q'
    while ( true )
    {
        // user moves or wants to quit playing
        if ( ! UserMoves() )
        {
            return;
        }

        m_Board.Draw();

        if ( m_Board.CheckForWinner() )
        {
            m_Board.Reset();
            std::cout << "You win -- New Game -- Press \'q\' at any time to quit" << std::endl;
            m_Board.Draw();
            continue;
        }

        // computer moves
        ComputerMoves();
        std::cout << "The Computer\'s move" << std::endl;
        m_Board.Draw();

        if ( m_Board.CheckForWinner() )
        {
            m_Board.Reset();
            std::cout << "You lose -- New Game -- Press \'q\' at any time to quit" << std::endl;
            m_Board.Draw();
            continue;
        }
    }
}

/******************************************************************************
 *
 * Entry point
 *
 *****************************************************************************/

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
int main()
{
    CTicTacToeGame Game;
    Game.Start();

done:
    std::cout << "exiting" << std::endl;
}
