import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Phase_1 {

    public static void main(String[] args) throws FileNotFoundException {
        OS object = new OS();
        object.load();
        object.printer();


    }
}


class OS{
    char [] buffer;
    char [][] memory;
    int mem_ptr;
    //CPU
    char [] IR;
    char [] R;
    int IC;
    boolean toggle;
    int SI; //system Interrupt



    public OS() throws FileNotFoundException {
        //size allocation
        buffer = new char[40];
        memory = new char[100][4];

        IR = new char[4];
        R = new char [4];

        value_allocator();

    }


    private void value_allocator() throws FileNotFoundException {
        //value allocation
        for(int i = 0;i<4;i++){
            IR[i] = '@';
            R[i] = '@';
        }
        buffer_reset();
        for(int i = 0;i < 100; i++){

            for(int j = 0; j< 4;j++){
                memory[i][j] = '@';
            }
        }
        IC = 0;
        toggle = false;
        mem_ptr = 0;
        SI = 0;


    }


    public void MOS(){
        switch (SI){
            case 1:
                try {
                    read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case 2:
                write();
                break;
            case 3:
                halt();
                break;

        }
    }


    public void read() throws IOException {
        System.out.println("Read executed");
        IR[3] = '0';
        int buffer_ptr;
        char [] temp;
        if(reader.ready()){
            temp = reader.readLine().toCharArray();
            for (buffer_ptr = 0; buffer_ptr < temp.length; buffer_ptr++) {
                buffer[buffer_ptr] = temp[buffer_ptr];
            }
            System.out.println(buffer);
        }
        if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {
            System.out.println("Program Over");
            return;
        }

        load_memory_data((IR[2]-'0')*10);
        buffer_reset();

    }


    public void write(){
        System.out.println("Write executed");
        IR[3] = '0';
        try{
            int memory_ptr = (IR[2]-'0')*10;
            int buffer_ptr4 = 0;
            int limit = memory_ptr+10;

            for(memory_ptr = (IR[2]-'0')*10;memory_ptr<limit;memory_ptr++){
                for(int i = 0;i<4;i++){
                    if(memory[memory_ptr][i] == '@')
                        continue;
                    buffer[buffer_ptr4] = memory[memory_ptr][i];
                    buffer_ptr4++;
                }
            }
            StringBuffer sb = new StringBuffer();
            buffer_ptr4 = 0;
            while(buffer_ptr4 < 40 && buffer[buffer_ptr4]!='@'){
                sb.append(buffer[buffer_ptr4]);
                buffer_ptr4++;
            }
            sb.append('\n');
            Files.write(Paths.get("out.txt"), String.valueOf(sb).getBytes(), StandardOpenOption.APPEND);


            buffer_reset();
        }
        catch(IOException e){
            System.out.println("Exception occurred at write()");
            e.printStackTrace();
        }


    }


    public void halt(){
        try{
            System.out.println("Halt called");
            Files.write(Paths.get("out.txt"), "\n\n".getBytes(), StandardOpenOption.APPEND);
        }
        catch(IOException e){
            System.out.println("Exception occurred at halt()");
            e.printStackTrace();
        }
    }


    public void MOSstartExec(){

        System.out.println("Exec started");
        IC = 0;
        executeUserProgram();   //slave mode
    }



    public void executeUserProgram(){
        //loading IR
        while (IC<90 && memory[IC][0] != '@') {
            for (int i = 0; i < 4; i++) {
                IR[i] = memory[IC][i];
            }
            IC++;
            switch (IR[0]) {
                case 'L':
                    if(IR[1] == 'R'){
                        for(int i = 0;i<4;i++){
                            R[i] = memory[(IR[2]-'0')*10 + (IR[3] -'0')][i];
                        }
                    }
                    break;
                case 'S':
                    if(IR[1] == 'R'){
                        for(int i = 0;i<4;i++){
                            memory[(IR[2]-'0')*10 + (IR[3] -'0')][i] = R[i];
                        }
                    }
                    break;
                case 'C':
                    if (IR[1] == 'R') {
                        int c = (IR[2]-'0') * 10 + (IR[3]-'0') ;
                        boolean flag = true;
                        for (int i = 0; i < 4; i++) {
                            if (R[i] != memory[c][i]) {
                                flag = false;
                                toggle = false;
                                break;
                            }
                        }
                        if(flag){
                            toggle = true;
                        }
                    }
                    break;

                case 'B':
                    if(IR[1] == 'T'){
                        if(toggle){
                            IC = (IR[2] - '0') *10 + (IR[3] - '0');
                        }
                    }
                    break;

                case 'G':
                    if (IR[1] == 'D') {
                        SI = 1;
                        MOS();
                    }
                    break;
                case 'P':
                    if (IR[1] == 'D') {
                        SI = 2;
                        MOS();
                    }
                    break;
                case 'H':
                    SI = 3;
                    MOS();
                    return;
            }
        }
    }






    private static java.io.File file;
    private static BufferedReader reader;
    static {
        try {
            file = new java.io.File("input.txt");
            reader = new BufferedReader(new FileReader(String.valueOf(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void load(){

        //loading from card to buffer

        try {
            do {
                int buffer_ptr;
                char [] temp;
                temp = reader.readLine().toCharArray();
                for (buffer_ptr = 0; buffer_ptr < temp.length; buffer_ptr++) {
                    buffer[buffer_ptr] = temp[buffer_ptr];
                }
                System.out.println(buffer);

                if (buffer[0] == '$' && buffer[1] == 'A' && buffer[2] == 'M' && buffer[3] == 'J') {
                    buffer_reset();
                    continue;
                }
                else if (buffer[0] == '$' && buffer[1] == 'D' && buffer[2] == 'T' && buffer[3] == 'A') {
                    buffer_reset();
                    mem_ptr = (mem_ptr % 10 == 0) ? mem_ptr : ((mem_ptr / 10 + 1) * 10);
                    MOSstartExec();
                }
                else if (buffer[0] == '$' && buffer[1] == 'E' && buffer[2] == 'N' && buffer[3] == 'D') {
                    System.out.println("Program Over");
                    printer();
                    value_allocator();
                }
                else{
                    load_memory_instructions();
                    buffer_reset();

                }
                if (buffer_ptr > 40) {
                    System.out.println("Buffer Overload, quitting...");
                    return;
                }
            }while(reader.ready());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void load_memory_instructions(){
        int buffer_ptr2 = 0;
        while(buffer_ptr2<40 && buffer[buffer_ptr2]!='@'){
            for(int i = 0;i<4;i++){
                if(buffer[buffer_ptr2] == '@')
                    break;
                memory[mem_ptr][i] = buffer[buffer_ptr2];
                if(buffer[buffer_ptr2]=='H'){
                    buffer_ptr2++;
                    break;
                }
                buffer_ptr2++;
            }
            mem_ptr++;
        }

    }


    public void load_memory_data(int location){
        mem_ptr = location;
        if(mem_ptr>=100){
            System.out.println("Memory Overload, quitting...");
            return;
        }
        int buffer_ptr1 = 0;
        while(buffer_ptr1<40 && buffer[buffer_ptr1]!='@'){
            for(int i = 0;i<4;i++){
                if(buffer[buffer_ptr1] == '@')
                    break;
                memory[mem_ptr][i] = buffer[buffer_ptr1];
                buffer_ptr1++;
            }
            mem_ptr++;
        }

    }


    public void buffer_reset(){
        for(int i = 0;i< 40;i++){
            buffer[i] = '@';
        }
    }







    public void printer(){
        System.out.println("Buffer is: ");
        System.out.println(buffer);
        System.out.println("IC is:");
        System.out.println(IC);
        System.out.println("Main memory");
        for(int i = 0; i< 100;i++){
            System.out.print(i+" : ");
            for(int j = 0;j<4;j++){
                System.out.print(memory[i][j]);
            }
            System.out.println();
        }
        System.out.println("IR:");
        System.out.println(IR);
        System.out.println("R:");
        System.out.println(R);
        System.out.println("Toggle:");
        System.out.println(toggle);

    }

}