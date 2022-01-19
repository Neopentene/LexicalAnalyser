import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

class lexicalAnalysis {
    public static void main(String[] args) throws IOException {
        File file = null;
        FileWriter writer = null;
        String filePath = null;
        boolean fileAvailable = true;

        try {
            LexcialAnalyser analyser = null;
            if (args.length == 0) {
                System.out.println("\n> No Input File Specified");
                System.out.println("\n> Scanning for file name \"scan_for_lexical.java\" on path "
                        + System.getProperty("user.home") + " --> Desktop");

                filePath = System.getProperty("user.home") + "/Desktop/scan_for_lexical.java";
                analyser = new LexcialAnalyser(filePath);

            } else {
                filePath = args[0];
                analyser = new LexcialAnalyser(filePath);
            }

            if (args.length <= 2 && args.length > 1) {
                System.out.println("\n> Loading the specified output file");
                file = new File(args[1]);
            } else {
                if (args.length > 2) {
                    throw new NullPointerException();
                }

                System.out.println("\n> No Output File Specified");

                try {
                    System.out.println("\n> Checking for file \"lexical_analysis_output.txt\" on path "
                            + System.getProperty("user.home") + " --> Desktop");
                    file = new File(System.getProperty("user.home") + "/Desktop/lexical_analysis_output.txt");

                    if (!file.canWrite()) {
                        if (!(new File(System.getProperty("user.home") + "/Desktop").exists()))
                            throw new FileNotFoundException("File is not writable or unavailable");
                    }

                } catch (Exception e) {
                    file = new File(
                            System.getProperty("user.home") + "/Lexical Output");

                    if (file.mkdirs() || file.canWrite()) {
                        System.out.println("> Directory Creation Successful");
                        System.out.println(
                                "> Output will be stored in Folder Lexical Output on path "
                                        + System.getProperty("user.home"));
                        file = new File(file.getPath() + "/lexical_analysis_output.txt");
                    } else {
                        System.out.println("> Failed to create a default directory");
                        System.out.println("> Since no output file is available, the output will not be stored");
                        fileAvailable = false;
                    }
                }

            }

            long stamp = 0;
            System.out.println("\n-- Starting Analysis --\n");
            stamp = new Date().getTime();

            HashMap<String, Long> output = analyser.analyseFile().getOutput();

            stamp = new Date().getTime() - stamp;

            if (fileAvailable) {

                if (file.createNewFile()) {
                    System.out.println("Creating a new output file:\n> " + file.getPath() + "\n");
                } else {
                    System.out.println("Writing to the available output file:\n> " + file.getPath() + "\n");
                }
                writer = new FileWriter(file);

                writer.write("Results for file\n--> " + analyser.getFile().getName() + "\n\n");
                writer.write("Target File Path\n--> " + analyser.getFile().getPath() + "\n\n");
                writer.write("Number of Tokens\n--> " + output.get("tokens") + "\n\n");
                writer.write("Identifiers\n--> " + output.get("identifiers") + "\n\n");
                writer.write("Keywords\n--> " + output.get("keywords") + "\n\n");
                writer.write("Operators\n--> " + output.get("operators") + "\n\n");
                writer.write("Delimiters\n--> " + output.get("delimiters") + "\n\n");
                writer.write("Characters\n--> " + output.get("characters") + "\n\n");
                writer.write("Strings\n--> " + output.get("strings") + "\n\n");
                writer.write("Comments\n--> " + output.get("comments") + "\n\n");
                writer.write("Lines\n--> " + output.get("lines") + "\n\n");

                writer.close();
            }

            System.out.println("Number of Tokens: " + output.get("tokens"));
            System.out.println("Identifiers: " + output.get("identifiers"));
            System.out.println("Keywords: " + output.get("keywords"));
            System.out.println("Operators: " + output.get("operators"));
            System.out.println("Delimiters: " + output.get("delimiters"));
            System.out.println("Characters: " + output.get("characters"));
            System.out.println("Strings: " + output.get("strings"));
            System.out.println("Comments: " + output.get("comments"));
            System.out.println("Lines: " + output.get("lines"));

            System.out.println("\nFinished in: " + stamp + " ms");

        } catch (FileNotFoundException notFoundException) {
            System.out.println("File not found");
        } catch (NullPointerException e) {
            System.out.println("No file specified");
        }
    }
}

class LexcialAnalyser {
    private HashMap<String, String> syntaxMap = null;
    private HashMap<String, String> commentMap = null;
    private Scanner reader = null;
    private File file = null;
    private JavaSyntax jSyntax = null;
    private LexicalOutput output = null;
    private boolean isString = false, isCharacter = false, isComment = false, wasOperator = false;

    LexcialAnalyser(String filePath) throws NullPointerException, FileNotFoundException {
        jSyntax = new JavaSyntax();
        syntaxMap = jSyntax.getSyntaxHashMap();
        commentMap = jSyntax.getCommentCategories();
        output = new LexicalOutput();
        file = new File(filePath);
        reader = new Scanner(file);
    }

    LexcialAnalyser(File file) throws FileNotFoundException {
        jSyntax = new JavaSyntax();
        syntaxMap = jSyntax.getSyntaxHashMap();
        commentMap = jSyntax.getCommentCategories();
        output = new LexicalOutput();
        this.file = file;
        reader = new Scanner(file);
    }

    LexcialAnalyser(Scanner reader) {
        jSyntax = new JavaSyntax();
        syntaxMap = jSyntax.getSyntaxHashMap();
        commentMap = jSyntax.getCommentCategories();

        output = new LexicalOutput();
        this.reader = reader;
    }

    public LexcialAnalyser analyseFile() throws IOException, IllegalStateException, NoSuchElementException {
        String line = null;
        while (reader.hasNextLine()) {
            line = reader.nextLine();
            analyseLine(line);
            output.increaseLine();
        }
        reader.close();
        return this;
    }

    public LexcialAnalyser analyseLine(String line) {
        String token = "", type = null;
        char lastCharacter = '\0';
        for (char character : line.toCharArray()) {
            type = analyseToken(Character.toString(character));
            switch (type) {

                case "string":
                    if (isComment)
                        break;

                    if (isString) {
                        unmarkString();
                        output.increaseString().increaseToken();
                        token = "";
                    } else {
                        markString();
                        if (wasOperator) {
                            unmarkOperator();
                            token = "";
                        }
                    }
                    break;

                case "character":
                    if (isComment || isString)
                        break;

                    if (isCharacter) {
                        unmarkCharacter();
                        output.increaseCharacter().increaseToken();
                        token = "";
                    } else {
                        markCharacter();
                        if (wasOperator) {
                            unmarkOperator();
                            token = "";
                        }
                    }
                    break;

                case "delimiter":
                    if (isComment || isString)
                        break;

                    if (wasOperator)
                        unmarkOperator();

                    setOutputOnToken(token);
                    output.increaseDelimiter().increaseToken();
                    token = "";
                    break;

                case "operator":
                    if (isString || isCharacter)
                        break;
                    if (isComment(lastCharacter + "" + character)) {
                        switch (getCommentType(lastCharacter + "" + character)) {
                            case "sl":
                                output.increaseComment().decreaseOperator().decreaseToken();
                                unmarkOperator();
                                return this;

                            case "mlo":
                                if (isComment)
                                    break;
                                output.increaseComment().decreaseOperator().decreaseToken();
                                markComment();
                                unmarkOperator();
                                character = '\0';
                                break;

                            case "mlc":
                                unmarkComment();
                                character = '\0';
                                token = "";
                        }
                        break;
                    }
                    if (!isComment) {

                        if (wasOperator) {

                            if (analyseToken(token + character).equals("operator")) {
                                token += character;
                                break;
                            }
                        }

                        output.increaseOperator().increaseToken();
                        setOutputOnToken(token);
                        markOperator();
                        token = "" + character;

                    }
                    break;

                case "space":

                    if (isString || isComment || isCharacter) {
                        token += character;
                        break;
                    }

                    if (wasOperator) {
                        unmarkOperator();
                        token = "";
                    }

                    if (token.isEmpty())
                        break;

                    setOutputOnToken(token);
                    token = "";
                    break;

                default:
                    if (wasOperator) {
                        unmarkOperator();
                        token = "";
                    }
                    token += character;

            }
            lastCharacter = character;
        }

        if (!token.isEmpty())
            setOutputOnToken(token);

        return this;
    }

    public String analyseToken(String token) {
        if (token.isEmpty() || token.equals("\n") || token.equals("\r"))
            return "empty";

        if (token.trim().length() == 0)
            return "space";

        String type = syntaxMap.get(token.trim());
        if ((type != null))
            return type;

        return "unknown";
    }

    public HashMap<String, Long> getOutput() {
        return output.getData();
    }

    public void resetOutput() {
        output = new LexicalOutput();
    }

    public File getFile() {
        return file;
    }

    private void setOutputOnToken(String token) {
        String type = analyseToken(token.trim());

        if (type.equals("keyword"))
            output.increaseKeyword().increaseToken();

        else if (type.equals("unknown"))
            output.increaseIdentifier().increaseToken();
    }

    private boolean isComment(String token) {
        if (token.isEmpty())
            return false;
        if (analyseToken(token).equals("comment"))
            return true;
        return false;
    }

    private String getCommentType(String token) {
        return commentMap.get(token);
    }

    private void markString() {
        isString = true;
    }

    private void unmarkString() {
        isString = false;
    }

    private void markCharacter() {
        isCharacter = true;
    }

    private void unmarkCharacter() {
        isCharacter = false;
    }

    private void markComment() {
        isComment = true;
    }

    private void unmarkComment() {
        isComment = false;
    }

    private void markOperator() {
        wasOperator = true;
    }

    private void unmarkOperator() {
        wasOperator = false;
    }
}

class LexicalOutput {
    private long tokens = 0, identifiers = 0, keywords = 0, operators = 0, delimiters = 0, comments = 0, strings = 0,
            characters = 0,
            lines = 0;

    public LexicalOutput increaseToken() {
        tokens++;
        return this;
    }

    public LexicalOutput increaseIdentifier() {
        identifiers++;
        return this;
    }

    public LexicalOutput increaseKeyword() {
        keywords++;
        return this;
    }

    public LexicalOutput increaseOperator() {
        operators++;
        return this;
    }

    public LexicalOutput increaseDelimiter() {
        delimiters++;
        return this;
    }

    public LexicalOutput increaseCharacter() {
        characters++;
        return this;
    }

    public LexicalOutput increaseString() {
        strings++;
        return this;
    }

    public LexicalOutput increaseComment() {
        comments++;
        return this;
    }

    public LexicalOutput increaseLine() {
        lines++;
        return this;
    }

    public LexicalOutput decreaseToken() {
        tokens--;
        return this;
    }

    public LexicalOutput decreaseIdentifier() {
        identifiers--;
        return this;
    }

    public LexicalOutput decreaseKeyword() {
        keywords--;
        return this;
    }

    public LexicalOutput decreaseOperator() {
        operators--;
        return this;
    }

    public LexicalOutput decreaseDelimiter() {
        delimiters--;
        return this;
    }

    public LexicalOutput decreaseCharacter() {
        characters--;
        return this;
    }

    public LexicalOutput decreaseString() {
        strings--;
        return this;
    }

    public LexicalOutput decreaseComment() {
        comments--;
        return this;
    }

    public LexicalOutput decreaseLine() {
        lines--;
        return this;
    }

    public long getTokens() {
        return tokens;
    }

    public long getIdentifiers() {
        return identifiers;
    }

    public long getKeywords() {
        return keywords;
    }

    public long getOperators() {
        return operators;
    }

    public long getDelimiters() {
        return delimiters;
    }

    public long getCharacters() {
        return characters;
    }

    public long getStrings() {
        return strings;
    }

    public long getComments() {
        return comments;
    }

    public long getLines() {
        return lines;
    }

    public HashMap<String, Long> getData() {
        HashMap<String, Long> data = new HashMap<String, Long>();
        data.put("tokens", (Long) tokens);
        data.put("identifiers", (Long) identifiers);
        data.put("keywords", (Long) keywords);
        data.put("operators", (Long) operators);
        data.put("delimiters", (Long) delimiters);
        data.put("characters", (Long) characters);
        data.put("strings", (Long) strings);
        data.put("comments", (Long) comments);
        data.put("lines", (Long) lines);
        return data;
    }
}

class JavaSyntax {
    private String keywords = "abstract continue for new switch assert default package synchronized boolean do if private this break double implements protected throw byte else import public throws case enum instanceof return transient catch extends int short try char final interface static void class finally long strictfp volatile float native super while";
    private String operators = "= + - * / % ++ -- ! == != > >= < <= && || ~ << >> >>> & ^ | += -=";
    private String delimiters = "( ) { } [ ] ; , . :";
    private String commentsDelimiters = "// /* */";
    private String stringLiterals = "\"";
    private String characterLiterals = "'";
    private String singleLineComments = "//";
    private String openMultiLineComments = "/*";
    private String closeMultiLineComments = "*/";

    public HashMap<String, String> getSyntaxHashMap() {
        HashMap<String, String> map = new HashMap<String, String>();

        for (String keyword : getListOfKeywords()) {
            map.put(keyword, "keyword");
        }
        for (String operator : getListOfOperators()) {
            map.put(operator, "operator");
        }
        for (String delimiter : getListOfDelimiters()) {
            map.put(delimiter, "delimiter");
        }
        for (String comment : getListOfCommentDelimiters()) {
            map.put(comment, "comment");
        }
        for (String string : getListOfStringLiterals()) {
            map.put(string, "string");
        }
        for (String character : getListOfCharacterLiterals()) {
            map.put(character, "character");
        }
        return map;
    }

    HashMap<String, String> getCommentCategories() {
        HashMap<String, String> map = new HashMap<String, String>();

        for (String comment : getListOfSingleLineCommentDelimiters()) {
            map.put(comment, "sl");
        }
        for (String comment : getListOfOpenMultiLineCommentDelimiters()) {
            map.put(comment, "mlo");
        }
        for (String comment : getListOfCloseMultiLineCommentDelimiters()) {
            map.put(comment, "mlc");
        }
        return map;
    }

    public List<String> getListOfKeywords() {
        return new ArrayList<String>(Arrays.asList(keywords.split("\\s")));
    }

    public List<String> getListOfOperators() {
        return new ArrayList<String>(Arrays.asList(operators.split("\\s")));
    }

    public List<String> getListOfDelimiters() {
        return new ArrayList<String>(Arrays.asList(delimiters.split("\\s")));
    }

    public List<String> getListOfCommentDelimiters() {
        return new ArrayList<String>(Arrays.asList(commentsDelimiters.split("\\s")));
    }

    public List<String> getListOfStringLiterals() {
        return new ArrayList<String>(Arrays.asList(stringLiterals.split("\\s")));
    }

    public List<String> getListOfCharacterLiterals() {
        return new ArrayList<String>(Arrays.asList(characterLiterals.split("\\s")));
    }

    public List<String> getListOfSingleLineCommentDelimiters() {
        return new ArrayList<String>(Arrays.asList(singleLineComments.split("\\s")));
    }

    public List<String> getListOfOpenMultiLineCommentDelimiters() {
        return new ArrayList<String>(Arrays.asList(openMultiLineComments.split("\\s")));
    }

    public List<String> getListOfCloseMultiLineCommentDelimiters() {
        return new ArrayList<String>(Arrays.asList(closeMultiLineComments.split("\\s")));
    }
}