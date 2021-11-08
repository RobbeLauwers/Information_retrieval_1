//From https://www.techiedelight.com/iterate-over-characters-string-java/
class Main
{
    // Iterate over the characters of a string
    public static void main(String[] args)
    {
        String s = "Techie Delight";

        // using simple for-loop
        for (int i = 0; i < s.length(); i++) {
            System.out.print(s.charAt(i));
        }
    }
}