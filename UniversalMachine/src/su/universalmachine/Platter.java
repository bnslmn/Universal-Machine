/*
 * @author: Amine Benslimane
 * 
 * March 2021, Sorbonne Université 
 * @mail: First.Last@etu.sorbonne-universite.fr
 * github: https://github.com/bnslmn
 * 
 * Implementing an Universal Machine of 32 bits, (ACM International Conference on Functional Programming (ICFP), 2006)
 * 
 * 
 * for executing the benchmark : configure sandmark.umz as an argument by following these steps :
 * 
 * -> right click on Platter.java --> Run as -> Run configuration
 * -> Select projet "UniversalMachine" and main class "su.universalmachine.Platter"
 * -> In arguments -> Program arguments : paste the location of "sandmark.umz"
 * 	
 * 																				Enjoy :)
 * 
 * */


package su.universalmachine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class Platter {
	static private int keys;

	private int reg[];
	private HashMap<Integer, int[]> linkMap;
	private int pc = 0;
	private LinkedList<Integer> doneInstr;
	private int[] platterInit;

	public Platter() {
		reg = new int[8];
		linkMap = new LinkedHashMap<>();
		doneInstr = new LinkedList<>();

	}

	public void execvm() {
		try {
			while (pc < platterInit.length) {
				int currentInstruction = platterInit[pc];

				// get the opcode (4 bits)
				int opcode = (currentInstruction >> 28) & 0b1111;

				// ****** treating special operator format ******

				// ORTH
				if (Objects.equals(opcode, 0b1101)) {
					int opNum = (currentInstruction >> 25) & 0b111;
					int value = (currentInstruction & 0b1111111111111111111111111);
					reg[opNum] = value;
					pc++;

				} else {

					// ****** normal operator format : OPCODE A B C ******

					int a = ((currentInstruction >> 6) & 0b111);
					int b = ((currentInstruction >> 3) & 0b111);
					int c = (currentInstruction & 0b111);

					switch (opcode) {
					// CMV
					case 0b0000:
						if (reg[c] != 0) {
							reg[a] = reg[b];
						}
						pc++;
						break;

					// AIND
					case 0b0001:

						int[] tmpIndex;
						if (reg[b] == 0) {
							tmpIndex = platterInit;
						} else {
							tmpIndex = linkMap.get(reg[b]);
						}

						reg[a] = tmpIndex[reg[c]];
						pc++;
						break;

					// AAMD
					case 0b0010:
						int[] tmpTab;
						if (reg[a] == 0) {
							tmpTab = platterInit;
						} else {
							tmpTab = linkMap.get(reg[a]);
						}
						tmpTab[reg[b]] = reg[c];

						pc++;
						break;

					// ADD
					case 0b0011:
						reg[a] = (reg[b] + reg[c]);
						pc++;
						break;

					// MULT
					case 0b0100:
						reg[a] = (reg[b] * reg[c]);
						pc++;
						break;
					// DIV
					case 0b0101: // 5
						int regC = reg[c];
						int regB = reg[b];
						reg[a] = Integer.divideUnsigned(regB, regC);
						pc++;
						break;

					// NAND
					case 0b0110:
						reg[a] = ~(reg[b] & reg[c]);
						pc++;
						break;

					// HALT
					case 0b0111:
						throw new RuntimeException();

					// ALLOC
					case 0b1000:
						// First fit search for a new identifier

						tmpTab = new int[reg[c]];

						// searching for a new identifier
						Integer key = doneInstr.pollFirst();

						// new key
						if (key == null) {
							keys++;
							key = keys;
						}
						linkMap.put(key, tmpTab);
						if (key == 0) {
							platterInit = tmpTab;
						}
						// register b receives the new identifier
						reg[b] = key;
						pc++;
						break;

					// ABANDON
					case 0b1001:
						linkMap.remove(reg[c]);
						doneInstr.add(reg[c]);
						pc++;
						break;

					// OUT
					case 0b1010:
						if (reg[c] >= 0 & reg[c] < 256)
							System.out.print((char) reg[c]);
						else
							throw new RuntimeException("out value not tolerated");
						pc++;
						break;

					// IN
					case 0b1011:
						byte bte;
						try {
							bte = (byte) System.in.read();
						} catch (Exception ex) {
							bte = (byte) '*'; // EOF
						}

						if (bte <= 0) {
							reg[c] = 0xFFFFFFFF;
						} else
							reg[c] = (int) bte;
						pc++;
						break;

					// LPROG
					case 0b1100:
						if (reg[b] != 0) {
							int array[] = linkMap.get(reg[b]);
							platterInit = array.clone();
							linkMap.put(0, platterInit);
						}
						pc = reg[c];
						break;
					}
				}
			}
		} catch (Exception e) {
		}
	}

	private int[] parseFile(String path) {
		File file;
		FileInputStream br = null;
		Byte initByte = (byte) 0;
		long timeIt = System.currentTimeMillis();

		try {
			file = new File(path);
			byte[] bt = new byte[(int) file.length()];
			br = new FileInputStream(file);
			//expected output by the benchmark
			System.out.println(br.read(bt));

			ByteBuffer btBuff = ByteBuffer.wrap(bt);
			for (int i = 0; i < file.length() % 4; ++i) {
				btBuff = btBuff.put(initByte);
			}
			IntBuffer intBuff = btBuff.asIntBuffer();
			
			int[] array = new int[intBuff.limit()];
			
			System.out.println(intBuff.get(array));
			
			System.out.println("Finished, time = " + (System.currentTimeMillis() - timeIt));
			return array;

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
			}
		}
		return new int[0];
	}

	public static void main(String args[]) {

		String file = args[0];
		Platter p = new Platter();
		int[] platters = p.parseFile(file);

		// program counter, contains the actual instruction
		p.pc = 0;
		// initializing the registers to 0
		Arrays.fill(p.reg, 0);
		p.platterInit = platters;
		p.linkMap.put(0, platters);

		long time = System.currentTimeMillis();
		p.execvm();
		System.out.println("Execution Time in s " + (System.currentTimeMillis() - time) / 1000);
	}

}
