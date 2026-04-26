namespace SlidingNumbersPlus.Models
{
    public class Tile
    {
        public int AbsPos { get; set; }
        public int Row { get; set; }
        public int Col { get; set; }
        public string Label { get; set; } = string.Empty;
        public bool IsEmpty => string.IsNullOrEmpty(Label);
        public bool IsCorrect => !IsEmpty && Label == AbsPos.ToString();
    }
}