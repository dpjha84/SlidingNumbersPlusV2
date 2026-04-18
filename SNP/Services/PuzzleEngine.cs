namespace SlidingNumbersPlus.Services
{
    public class Tile
    {
        public int    AbsPos  { get; set; }
        public int    Row     { get; set; }
        public int    Col     { get; set; }
        public string Label   { get; set; } = string.Empty;
        public bool   IsEmpty   => string.IsNullOrEmpty(Label);
        public bool   IsCorrect => !IsEmpty && Label == AbsPos.ToString();
    }

    public class PuzzleEngine
    {
        private readonly Random _rng = new();
        private Tile[,] _grid = new Tile[0, 0];
        private Tile    _empty = new();
        private int     _size;
        public  int     Size => _size;

        public void Init(int size, List<string> seq)
        {
            _size = size;
            _grid = new Tile[size, size];
            int k = 0;
            for (int r = 0; r < size; r++)
                for (int c = 0; c < size; c++)
                {
                    var t = new Tile { AbsPos = k + 1, Row = r, Col = c,
                                       Label  = k < seq.Count ? seq[k] : string.Empty };
                    _grid[r, c] = t;
                    if (t.IsEmpty) _empty = t;
                    k++;
                }
        }

        public List<Tile> AllTiles()
        {
            var list = new List<Tile>(_size * _size);
            for (int r = 0; r < _size; r++)
                for (int c = 0; c < _size; c++)
                    list.Add(_grid[r, c]);
            return list;
        }

        public List<string> Sequence() => AllTiles().Select(t => t.Label).ToList();

        // Returns changed tiles, or empty list if move invalid
        public List<Tile> TryMove(int row, int col)
        {
            var changed = new List<Tile>();
            var t = _grid[row, col];
            if (t.IsEmpty) return changed;

            int dr = 0, dc = 0;
            if (_empty.Row == row && _empty.Col != col)
                dc = _empty.Col > col ? 1 : -1;
            else if (_empty.Col == col && _empty.Row != row)
                dr = _empty.Row > row ? 1 : -1;
            else
                return changed;

            var chain = new List<Tile>();
            int r = row, c = col;
            while (r != _empty.Row || c != _empty.Col)
            {
                chain.Add(_grid[r, c]);
                r += dr; c += dc;
            }

            for (int i = chain.Count - 1; i >= 0; i--)
            {
                var dest = (i == chain.Count - 1) ? _empty : chain[i + 1];
                dest.Label = chain[i].Label;
                changed.Add(dest);
            }
            chain[0].Label = string.Empty;
            _empty = chain[0];
            changed.Add(_empty);
            return changed;
        }

        public bool IsSolved()
        {
            for (int r = 0; r < _size; r++)
                for (int c = 0; c < _size; c++)
                {
                    var t = _grid[r, c];
                    if (!t.IsEmpty && t.Label != t.AbsPos.ToString()) return false;
                }
            return true;
        }

        public void Shuffle(int moves)
        {
            Tile? prev = null;
            for (int i = 0; i < moves; i++)
            {
                var neighbours = GetNeighbours(_empty);
                var cands = neighbours.Where(x => x != prev).ToList();
                if (cands.Count == 0) cands = neighbours;
                var pick = cands[_rng.Next(cands.Count)];
                prev = _grid[pick.Row, pick.Col].IsEmpty ? null : _empty;
                TryMove(pick.Row, pick.Col);
            }
        }

        private List<Tile> GetNeighbours(Tile t)
        {
            var list = new List<Tile>();
            if (t.Row > 0)        list.Add(_grid[t.Row - 1, t.Col]);
            if (t.Row < _size-1)  list.Add(_grid[t.Row + 1, t.Col]);
            if (t.Col > 0)        list.Add(_grid[t.Row, t.Col - 1]);
            if (t.Col < _size-1)  list.Add(_grid[t.Row, t.Col + 1]);
            return list;
        }
    }
}
