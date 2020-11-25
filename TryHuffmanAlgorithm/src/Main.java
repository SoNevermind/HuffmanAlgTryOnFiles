import java.io.*;
import java.nio.charset.CoderMalfunctionError;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try{
            String content = new String(Files.readAllBytes(Paths.get("testText.txt"))); //Загрузка содержимого файла в виде строки

            //Вычисление таблицы частот с которыми встречаются символы в тексте
            TreeMap<Character, Integer> frequencies = countFrequency(content);
            ArrayList<codeTreeNode> codeTreeNodes = new ArrayList<>();

            //Генерация узлов будущего дерева для узлов символов текста
            for(Character ch : frequencies.keySet()){
                codeTreeNodes.add(new codeTreeNode(ch, frequencies.get(ch)));
            }

            //Построение кодового дерева с помощью алгоритма
            codeTreeNode treeNode = huffman(codeTreeNodes);

            //Построение таблицы префексных кодов для символов исходного текста
            TreeMap<Character, String> codes = new TreeMap<>();
            for(Character ch : frequencies.keySet()){
                codes.put(ch, treeNode.getCodeForCharacter(ch, ""));
            }

            //Кодирование текста префексными кодами
            StringBuilder encoded = new StringBuilder();
            for (int i = 0; i < content.length(); i++){
                encoded.append(codes.get(content.charAt(i)));
            }

            //Сохранение сжатой информации в файл
            File file = new File("compressed.huf");
            saveToFile(file, frequencies, encoded.toString());

            TreeMap<Character, Integer> frequencies2 = new TreeMap<>();
            StringBuilder encoded2 = new StringBuilder();
            codeTreeNodes.clear();

            //Извлечение сжатой информации из файла
            loadFromFile(file, frequencies2, encoded2);

            //Генерация узлов и построение дерева на основе таблицы частот сжатого файла
            for(Character character : frequencies2.keySet()){
                codeTreeNodes.add(new codeTreeNode(character, frequencies2.get(character)));
            }

            codeTreeNode treeNode2 = huffman(codeTreeNodes);

            //Декодирование исходной информации
            String decoded = huffmanDecode(encoded2.toString(), treeNode2);

            //Сохранение декодированой информации в файл
            Files.write(Paths.get("decompressed.txt"), decoded.getBytes());
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    //Подсчет того, сколько раз какой символ встречается в тексте (ключом является символ, а данными количество того, сколько раз символ встречается в тексте)
    public static TreeMap<Character, Integer> countFrequency(String text){
        TreeMap<Character, Integer> freqMap = new TreeMap<>();

        for(int i = 0; i < text.length(); i++){
            Character c = text.charAt(i);
            Integer count = freqMap.get(c);
            freqMap.put(c, count != null ? count + 1 : 1);
        }

        return freqMap;
    }

    //Алгоритм Хаффмана (функция будет возвращать дерево и в качестве аргументов принимает список узлов для листов дерева с символами
    public static codeTreeNode huffman(ArrayList<codeTreeNode> codeTreeNodes){
        while(codeTreeNodes.size() > 1){
            //Упорядочивание узлов по весам
            Collections.sort(codeTreeNodes);

            //Берем два узла с самыми маленькими весами
            codeTreeNode left = codeTreeNodes.remove(codeTreeNodes.size() - 1); //Получаем из списка узел и тут же его удаляем
            codeTreeNode right = codeTreeNodes.remove(codeTreeNodes.size() - 1);

            //Промежуточный узел
            codeTreeNode parent = new codeTreeNode(null, right.weight + left.weight, left, right);
            codeTreeNodes.add(parent);
        }

        return codeTreeNodes.get(0);
    }

    //Метод декодирования
    private static String huffmanDecode(String encoded, codeTreeNode treeNode){
        StringBuilder decoded = new StringBuilder(); //накапливаем расшифрованные данные

        //Хранение текущего узла при спуске по дереву
        codeTreeNode node = treeNode;
        //идем по битам зашифрованной строки
        for(int i = 0; i < encoded.length(); i++){
            node = encoded.charAt(i) == '0' ? node.left : node.right;

            if(node.content != null){
                decoded.append(node.content);
                node = treeNode;
            }
        }

        return decoded.toString();
    }


    //Кодовое дерево
    private static class codeTreeNode implements Comparable<codeTreeNode>{

        Character content; //символ
        int weight; //вес (сумма дочерних узлов для промежуточного узла)
        codeTreeNode left, right; //потомки

        public codeTreeNode(Character content, int weight) {
            this.content = content;
            this.weight = weight;
        }

        public codeTreeNode(Character content, int weight, codeTreeNode left, codeTreeNode right) {
            this.content = content;
            this.weight = weight;
            this.left = left;
            this.right = right;
        }

        @Override
        public int compareTo(codeTreeNode codeTreeNode){
            return codeTreeNode.weight - weight; //у кого веса больше, тот и на первом месте (сортировка по убыванию)
        }

        //Проход по дереву от корня до конкретного символа и при этом по поворотам вычислять 0 и 1, которые будут кодом данного символа
        public String getCodeForCharacter(Character ch, String parentPath){
            //параметры функции принимают символ, для которого ищется код и путь в виде 0 и 1

            if(content == ch){
                return parentPath;
            } else {
                if(left != null){
                    String path = left.getCodeForCharacter(ch, parentPath + 0);

                    if(path != null){
                        return path;
                    }
                }

                if(right != null){
                    String path = right.getCodeForCharacter(ch, parentPath + 1);

                    if(path != null){
                        return path;
                    }
                }
            }

            return null;

            //Алгоритм обхода дерева в глубину. Для всех листов, которые не соответсвуют поиску, будет возвращен null в качестве кода и только для одного узла, который нужен вернется какой-то код и он при обходе по дереву всплывет наверх и будет возвращен в качестве результата вызова верхней функции
        }
    }

    //Реализация битового массива
    public static class bitArray{
        int size;
        byte[] bytes;

        private byte[] masks = new byte[] {0b00000001, 0b00000010, 0b00000100, 0b00001000, 0b00010000, 0b00100000, 0b01000000, (byte) 0b10000000};

        public bitArray(int size){
            this.size = size;

            int sizeInBytes = size / 8;
            if(size % 8 > 0){
                sizeInBytes += 1;
            }

            bytes = new byte[sizeInBytes];
        }

        public bitArray(int size, byte[] bytes){
            this.size = size;
            this.bytes = bytes;
        }

        public int get(int index){
            int byteIndex = index / 8;
            int bitIndex = index % 8;

            return (bytes[byteIndex] & masks[bitIndex]) != 0 ? 1 : 0;
        }

        public void set(int index, int value){
            int byteIndex = index / 8;
            int bitIndex = index % 8;

            if(value != 0){
                bytes[byteIndex] = (byte) (bytes[byteIndex] | masks[bitIndex]);
            } else {
                bytes[byteIndex] = (byte) (bytes[byteIndex] & ~masks[bitIndex]);
            }
        }

        @Override
        public String toString(){
            StringBuilder stringBuilder = new StringBuilder();

            for(int i = 0; i < size; i++){
                stringBuilder.append(get(i) > 0 ? '1' : '0');
            }

            return stringBuilder.toString();
        }

        public int getSize(){
            return size;
        }

        public int getSizeInBytes(){
            return bytes.length;
        }

        public byte[] getBytes(){
            return bytes;
        }
    }

    //Сохранение таблицы частот и сжатой информации в файл
    private static void saveToFile(File output, Map<Character, Integer> frequencies, String bits){
        try{
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(output));
            outputStream.writeInt(frequencies.size());

            for(Character character : frequencies.keySet()){
                outputStream.writeChar(character);
                outputStream.writeInt(frequencies.get(character));
            }

            int compressedSizeBits = bits.length();
            bitArray bitArr = new bitArray(compressedSizeBits);

            for(int i = 0; i < bits.length(); i++){
                bitArr.set(i, bits.charAt(i) != '0' ? 1 : 0);
            }

            outputStream.writeInt(compressedSizeBits);
            outputStream.write(bitArr.bytes, 0, bitArr.getSizeInBytes());
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException exception){
            exception.printStackTrace();
        } catch (IOException exception){
            exception.printStackTrace();
        }
    }

    //Загрузка сжатой информации и таблицы частот из файла
    private static void loadFromFile(File input, Map<Character, Integer> frequencies, StringBuilder bits){
        try{
            DataInputStream inputStream = new DataInputStream(new FileInputStream(input));

            int freqTableSize = inputStream.readInt();

            for(int i = 0; i < freqTableSize; i++){
                frequencies.put(inputStream.readChar(), inputStream.readInt());
            }

            int dataSizeBits = inputStream.readInt();
            bitArray bitArr = new bitArray(dataSizeBits);

            inputStream.read(bitArr.bytes, 0, bitArr.getSizeInBytes());
            inputStream.close();

            for(int i = 0; i < bitArr.size; i++){
                bits.append(bitArr.get(i) != 0 ? "1" : 0);
            }

        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}

