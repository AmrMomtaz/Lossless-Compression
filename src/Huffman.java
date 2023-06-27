import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Huffman implements CompressionAlgorithm{
    Node decompressRoot = null;
    private class Node{
        String unit;
        int freq;
        Node left;
        Node right;
        private Node(String unit, int freq, Node left, Node right){
            this.unit = unit;
            this.freq = freq;
            this.left = left;
            this.right = right;
        }
    }

    ///////////////////////////////////  COMPRESSION //////////////////////////////////////////////////////////
    @Override
    public void compress(String path) {
        compress(path, 1);
    }

    public String getCompressedPath(String path) {
        return getCompressedPath(path, 1);
    }

    public String getCompressedPath(String inputPath, int n){
        inputPath = inputPath.replace("\\", "/");
        String newPath = "";
        String[] pathComponents = inputPath.split("/");
        String inputFileName = pathComponents[pathComponents.length-1];
        String outputFileName = String.valueOf(n) + "." + inputFileName + ".hc";
        for(int i=0; i<pathComponents.length-1; i++)
            newPath += pathComponents[i] + "/";
        newPath += outputFileName;
        return newPath;
    }
    public String getDecompressedPath(String inputPath){
        inputPath = inputPath.replace("\\", "/");
        String newPath = "";
        String[] pathComponents = inputPath.split("/");
        String inputFileName = pathComponents[pathComponents.length-1];
        String outputFileName = "extracted." + inputFileName.substring(0, inputFileName.length()-3);
        for(int i=0; i<pathComponents.length-1; i++)
            newPath += pathComponents[i] + "/";
        newPath += outputFileName;
        return newPath;
    }
    private void compress(String path, int n){
        int[] numUnits = {0}; // number of units processed
        int[] sizeLast = {Integer.MAX_VALUE}; // size of the last byte, which is less than n if file size is not divisible by n
        HashMap<String, Integer> freqTable = generateFreqTable(path, n, numUnits, sizeLast);
        Node[]nodes = generateNodes(freqTable);
        Node root = generateTree(nodes);
        HashMap<String, String> codes = new HashMap<>();
        generateCode(codes, root, "");
        writeCompressed(codes, path, root, n, numUnits, sizeLast);
    }

    // create a frequency table of all the units in the file
    private HashMap<String, Integer> generateFreqTable(String path, int n, int[] numUnits, int[] sizeLast){
        HashMap<String, Integer> freqTable = new HashMap<>();
        // read file byte by byte and count the chunks
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path))){
            int br;
            while((br = bis.read()) != -1){
                StringBuilder unit = new StringBuilder();
                unit.append((char)br);
                int currSize;
                for(currSize=0; currSize < n-1 && ((br = bis.read()) != -1); currSize++)
                    unit.append((char) br);

                numUnits[0]++;
                if(sizeLast[0] > currSize) sizeLast[0] = currSize+1;

                // check if exists in table, if not then create new entry and put one
                // else increment the frequency
                String unitString = unit.toString();
                if(freqTable.containsKey(unitString)){
                    freqTable.put(unitString, freqTable.get(unitString)+1);
                } else {
                    freqTable.put(unitString, 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return freqTable;
    }

    // create an array of nodes using the frequency table
    private Node[] generateNodes(HashMap<String, Integer> freqTable){
        Node[] nodes = new Node[freqTable.size()];
        int index = 0;
        for(String unit : freqTable.keySet())
            nodes[index++] = new Node(unit, freqTable.get(unit), null, null);
        return nodes;
    }

    // create the binary tree using the nodes carrying the units and their frequencies
    private Node generateTree(Node[] nodes){
        Node node1, node2, tempNode, root = null;
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(node -> node.freq));
        // fill the queue
        Collections.addAll(pq, nodes);

        while(pq.size() > 1){
            node1 = pq.poll();
            node2 = pq.poll();

            tempNode = new Node("", node1.freq, node1, node2);
            if(node2 != null) tempNode.freq += node2.freq;

            pq.add(tempNode);
            root = tempNode;
        }
        return root;
    }

    // traverse inorder to generate the code
    private void generateCode(HashMap<String, String> codes, Node node, String code){
        if(node == null) return;
        generateCode(codes, node.left,code + "0");
        if(node.unit.length() != 0) codes.put(node.unit, code);
        generateCode(codes, node.right, code + "1");
    }

    private String getFirstLine(int n, int[] numUnits, int[] sizeLast){
        String line = "";
        line += String.valueOf(n) + '-' + numUnits[0] + '-' + sizeLast[0] + '\n';
        return line;
    }

    private String getBfsTree(Node root, int n){
        StringBuilder tree = new StringBuilder();
        Queue<Node> nodes = new LinkedList<>();
        nodes.add(root);
        while(!nodes.isEmpty()){
            Node node = nodes.poll();
            String nodeUnit = node.unit;
            // not leaf node
            if(nodeUnit.length() == 0){
                tree.append('0');

            } else {
                //leaf node
                // write the size
                // then write the content

                if(nodeUnit.length() == n) tree.append('1').append(nodeUnit);
                    // maybe because of this we'll need to add its size just to avoid overshooting
                    // when trying to read the tree?
                else tree.append('2').append(nodeUnit);
            }
            if(node.left != null) nodes.add(node.left);
            if(node.right != null) nodes.add(node.right);
        }
        tree.append('#');
        return tree.toString();
    }

    private void writeCompressed(HashMap<String, String> codes, String path, Node root, int n, int[] numUnits, int[] sizeLast){
        // first write your tree : c
        // // write n first to know how many bytes to read
        // // write the last size of the last one
        // second read byte by byte : d
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path))) {
            try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(getCompressedPath(path, n)))) {

                // deal with writing the tree
                // write the meta data
                // n - number of units - size of last byte
                //
                String line = getFirstLine(n, numUnits, sizeLast);
                String tree = getBfsTree(root, n);
                bos.write(line.getBytes(StandardCharsets.ISO_8859_1));
                bos.write(tree.getBytes(StandardCharsets.ISO_8859_1));
                //reading the file and writing in the compressed
                int bytePtr = 0;

                int[] bs = {0, 0, 0, 0, 0, 0, 0, 0};
                int br;
                while ((br = bis.read()) != -1) {
                    int counter = 0;
                    StringBuilder unit = new StringBuilder();
                    // find unit
                    unit.append((char)br);
                    while (counter++ < n-1 && (br = bis.read()) != -1)
                        unit.append((char) br);
                    // create bitset and store its value in it
                    String code = codes.get(unit.toString());
                    int codePtr = 0;
                    while(codePtr < code.length()){
                        for(; bytePtr < 8 ; bytePtr++){
                            if(codePtr >= code.length()) break;

                            if(code.charAt(codePtr++) == '1'){
                                bs[bytePtr] = 1;
                            }
                        }
                        // it is full
                        if(bytePtr == 8) {
                            bytePtr = 0;
                            bos.write(encodeToByte(bs));
                            bs = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
                        }
                    }
                    // then convert to byte array and write in file
                }
                if(bytePtr != 0) bos.write(encodeToByte(bs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // // every time there's a hit we'll write to the file
        // convert each n bytes into their code and write in compressed file : d-c (A lot of work)
    }

    private byte encodeToByte(int[] bits) {
        byte result = 0;
        int byteValue = 0;
        int index;
        for (index = 0; index < bits.length; index++) {
            byteValue = (byteValue << 1) | bits[index];
            result = (byte) byteValue;
        }
        if (index % 8 != 0) {
            result = (byte) ((byte) byteValue << (8 - (index % 8)));
        }
        return result;
    }

    ///////////////////////////////////  DECOMPRESSION //////////////////////////////////////////////////////////

    private void insertNode(Queue<Node> q, String unit){
        Node node = new Node(unit, 0, null, null);
        if(decompressRoot == null){
            decompressRoot = node;
        }
        else if(q.peek().left == null){
            q.peek().left = node;
        }else{
            // if we filled the right this means that we no longer have to add anything so we can remove it
            q.peek().right = node;
            q.remove();
        }
        if(node.unit.length() == 0) q.add(node);

    }
    private void generateCodeReversed(HashMap<String,String> codes, Node node, String code){
        if(node == null) return;
        generateCodeReversed(codes, node.left,code + "0");
        if(node.unit.length() != 0) codes.put(code, node.unit);
        generateCodeReversed(codes, node.right, code + "1");
    }
    @Override
    public void decompress(String path){
        StringBuilder numHolder = new StringBuilder();
        int n, numUnits, sizeLast;
        // read n till -
        // read number till -
        // read remaining number till \n
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path))){

            int readByte;
            // read n
            while((readByte = bis.read()) != -1){
                if((char)readByte == '-') break;
                numHolder.append((char)readByte);
            }
            n = Integer.parseInt(numHolder.toString());
            numHolder = new StringBuilder();
            while((readByte = bis.read()) != -1){
                if((char)readByte == '-') break;
                numHolder.append((char)readByte);
            }
            numUnits = Integer.parseInt(numHolder.toString());
            numHolder = new StringBuilder();
            while((readByte = bis.read()) != -1){
                if((char)readByte == '\n') break;
                numHolder.append((char)readByte);
            }
            sizeLast = Integer.parseInt(numHolder.toString());
            // now read the tree
            Queue<Node> q = new LinkedList<>();
            // read
            // if 0 then enter non leaf node
            // if 1 then read n unit
            // if 2 then read sizeLast chunk
            // if # then END
            while(true){
                readByte = (char)bis.read();
                if(readByte == '#') break;
                else if(readByte == '0'){ // non leaf node
                    insertNode(q, "");
                }else if(readByte == '1'){
                    StringBuilder complete = new StringBuilder();
                    for(int i=0; i<n; i++) complete.append((char) bis.read());
                    insertNode(q, complete.toString());
                }else if(readByte == '2'){
                    StringBuilder incomplete = new StringBuilder();
                    for(int i=0; i<sizeLast; i++) incomplete.append((char) bis.read());
                    insertNode(q, incomplete.toString());
                }
            }
            HashMap<String, String> codeToString = new HashMap<>();
            generateCodeReversed(codeToString, decompressRoot, "");

            // now to read the contents of the file and write the corresponding words
            try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(getDecompressedPath(path)))){
                String compared = "";
                int br;
                while((br = bis.read()) != -1){
                    String temp = Integer.toBinaryString((char)br);
                    StringBuilder symbol = new StringBuilder();
                    symbol.append("0".repeat(Math.max(0, 8 - temp.length())));
                    symbol.append(temp);
                    for(int i=0; i<8; i++){
                        compared += symbol.charAt(i);
                        if(codeToString.containsKey(compared) && numUnits != 0){
                            bos.write(codeToString.get(compared).getBytes(StandardCharsets.ISO_8859_1));
                            compared = "";
                            numUnits--;
                        }
                    }

                }
                while(numUnits != 0){
                    if(codeToString.containsKey(compared)){

                        bos.write(codeToString.get(compared).getBytes(StandardCharsets.ISO_8859_1));
                        numUnits--;
                        break;
                    }
                    compared += '0';
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
