package edu.vt.cs.append;

import java.util.Random;

public class HungarianAlgorithm {
	  public HungarianAlgorithm()
	    {
	    }

	    public static void generateRandomArray(double[][] array, String randomMethod)
	    {
	        Random generator = new Random();
	        for(int i = 0; i < array.length; i++)
	        {
	            for(int j = 0; j < array[i].length; j++)
	            {
	                if(randomMethod.equals("random"))
	                    array[i][j] = generator.nextDouble();
	                if(!randomMethod.equals("gaussian"))
	                    continue;
	                array[i][j] = generator.nextGaussian() / 4D;
	                if(array[i][j] > 0.5D)
	                    array[i][j] = 0.5D;
	                if(array[i][j] < -0.5D)
	                    array[i][j] = -0.5D;
	                array[i][j] = array[i][j] + 0.5D;
	            }

	        }

	    }

	    public static double findLargest(double[][] array)
	    {
	        double largest = 0.0D;
	        for(int i = 0; i < array.length; i++)
	        {
	            for(int j = 0; j < array[i].length; j++)
	                if(array[i][j] > largest)
	                    largest = array[i][j];

	        }

	        return largest;
	    }

	    public static double[][] transpose(double[][] array)
	    {
	        double[][] transposedArray = new double[array[0].length][array.length];
	        for(int i = 0; i < transposedArray.length; i++)
	        {
	            for(int j = 0; j < transposedArray[i].length; j++)
	                transposedArray[i][j] = array[j][i];

	        }

	        return transposedArray;
	    }

	    public static double[][] copyOf(double[][] original)
	    {
	        double[][] copy = new double[original.length][original[0].length];
	        for(int i = 0; i < original.length; i++)
	            System.arraycopy(original[i], 0, copy[i], 0, original[i].length);

	        return copy;
	    }

	    public int[][] hgAlgorithm(double[][] array)
	    {
	        double[][] cost = copyOf(array);
	        double maxCost = findLargest(cost);
	        int[][] mask = new int[cost.length][cost[0].length];
	        int[] rowCover = new int[cost.length];
	        int[] colCover = new int[cost[0].length];
	        int[] zero_RC = new int[2];
	        int step = 1;
	        boolean done = false;
	        do
	            if(!done)
	            {
	                switch(step)
	                {
	                case 1: // '\001'
	                    step = hg_step1(step, cost);
	                    break;

	                case 2: // '\002'
	                    step = hg_step2(step, cost, mask, rowCover, colCover);
//	                    printMask(mask,"2");
	                    break;

	                case 3: // '\003'
	                    step = hg_step3(step, mask, colCover);
//	                    printMask(mask,"3");
	                    break;

	                case 4: // '\004'
	                    step = hg_step4(step, cost, mask, rowCover, colCover, zero_RC);
//	                    printMask(mask,"4");
	                    break;

	                case 5: // '\005'
	                    step = hg_step5(step, mask, rowCover, colCover, zero_RC);
//	                    printMask(mask,"5");
	                    break;

	                case 6: // '\006'
	                    step = hg_step6(step, cost, rowCover, colCover, maxCost);
//	                    printMask(mask,"6");
	                    break;

	                case 7: // '\007'
	                    done = true;
	                    break;
	                }
	            } else
	            {
	                int[][] assignment = new int[array.length][2];
	                for(int i = 0; i < mask.length; i++)
	                {
	                    for(int j = 0; j < mask[i].length; j++)
	                        if(mask[i][j] == 1)
	                        {
	                            assignment[i][0] = i;
	                            assignment[i][1] = j;
	                        }

	                }

	                return assignment;
	            }
	        while(true);
	    }

	    public double hgAlgorithmOnlyCost(double[][] array)
	    {
	        double[][] cost = copyOf(array);
	        double maxCost = findLargest(cost);
	        int[][] mask = new int[cost.length][cost[0].length];
	        int[] rowCover = new int[cost.length];
	        int[] colCover = new int[cost[0].length];
	        int[] zero_RC = new int[2];
	        int step = 1;
	        boolean done = false;
	        do
	            if(!done)
	            {
	                switch(step)
	                {
	                case 1: // '\001'
	                    step = hg_step1(step, cost);
	                    break;

	                case 2: // '\002'
	                    step = hg_step2(step, cost, mask, rowCover, colCover);
	                    printMask(mask,"2");
	                    break;

	                case 3: // '\003'
	                    step = hg_step3(step, mask, colCover);
	                    printMask(mask,"3");
	                    break;

	                case 4: // '\004'
	                    step = hg_step4(step, cost, mask, rowCover, colCover, zero_RC);
	                    printMask(mask,"4");
	                    break;

	                case 5: // '\005'
	                    step = hg_step5(step, mask, rowCover, colCover, zero_RC);
	                    printMask(mask,"5");
	                    break;

	                case 6: // '\006'
	                    step = hg_step6(step, cost, rowCover, colCover, maxCost);
	                    printMask(mask,"6");
	                    break;

	                case 7: // '\007'
	                    done = true;
	                    break;
	                }
	            } else
	            {
	                int[][] assignment = new int[array.length][2];
	                for(int i = 0; i < mask.length; i++)
	                {
	                    for(int j = 0; j < mask[i].length; j++)
	                        if(mask[i][j] == 1)
	                        {
	                            assignment[i][0] = i;
	                            assignment[i][1] = j;
	                        }

	                }

	                double sum = 0.0D;
	                for(int i = 0; i < assignment.length; i++)
	                    sum += array[assignment[i][0]][assignment[i][1]];

	                return sum;
	            }
	        while(true);
	    }

	    public static int hg_step1(int step, double[][] cost)
	    {
	        for(int i = 0; i < cost.length; i++)
	        {
	            double minval = cost[i][0];
	            for(int j = 1/*optimized by nameng*/; j < cost[i].length; j++)//by nameng: to find the minval in each row
	                if(minval > cost[i][j])
	                    minval = cost[i][j];
	            for(int j = 0; j < cost[i].length; j++)//by nameng: to find the relative cost
	            	cost[i][j] = cost[i][j] - minval;	            
	        }

	        step = 2;
	        return step;
	    }

	    public static int hg_step2(int step, double[][] cost, int[][] mask, int[] rowCover, int[] colCover)
	    {	    	
	        for(int i = 0; i < cost.length; i++)
	        {
	        	System.out.print("");
	            for(int j = 0; j < cost[i].length; j++){
  	              if(cost[i][j] == 0.0D && colCover[j] == 0 && rowCover[i] == 0)
	                {//by nameng, if the cell leads to the least cost, the colCover[j] and rowCover[i] are marked as 1
	                    mask[i][j] = 1;
	                    colCover[j] = 1;
	                    rowCover[i] = 1;
	                }
	            }
	        }

	        clearCovers(rowCover, colCover); //by nameng: clear covers
	        step = 3;
	        return step;
	    }

	    private static void printMask(int[][] mask, String iteration) {
			// TODO Auto-generated method stub
			System.out.println("mapping:"+iteration);
			for(int i=0; i<mask.length; i++){
				for(int j=0; j<mask[i].length; j++){
					if(mask[i][j]==1){
						System.out.println(i+"<->"+j);
					}
				}
			}
			System.out.println("========================");
		}

		public static int hg_step3(int step, int[][] mask, int[] colCover)
	    {
	        for(int i = 0; i < mask.length; i++)
	        {
	            for(int j = 0; j < mask[i].length; j++)
	                if(mask[i][j] == 1)
	                    colCover[j] = 1;
	  
	        }

	        int count = 0;
	        for(int j = 0; j < colCover.length; j++)
	            count += colCover[j];

	        if(count >= mask.length)
	            step = 7;
	        else
	            step = 4;
	        return step;
	    }

	    public static int hg_step4(int step, double[][] cost, int[][] mask, int[] rowCover, int[] colCover, int[] zero_RC)
	    {
	        int[] row_col = new int[2];
	        boolean done = false;
	        while(!done) 
	        {
	            row_col = findUncoveredZero(row_col, cost, rowCover, colCover);
	            if(row_col[0] == -1)
	            {
	                done = true;
	                step = 6;
	            } else
	            {
	                mask[row_col[0]][row_col[1]] = 2;
	                boolean starInRow = false;
	                for(int j = 0; j < mask[row_col[0]].length; j++)
	                    if(mask[row_col[0]][j] == 1)
	                    {
	                        starInRow = true;
	                        row_col[1] = j;
	                    }

	                if(starInRow)
	                {
	                    rowCover[row_col[0]] = 1;
	                    colCover[row_col[1]] = 0;
	                } else
	                {
	                    zero_RC[0] = row_col[0];
	                    zero_RC[1] = row_col[1];
	                    done = true;
	                    step = 5;
	                }
	            }
	        }
	        return step;
	    }

	    public static int[] findUncoveredZero(int[] row_col, double[][] cost, int[] rowCover, int[] colCover)
	    {
	        row_col[0] = -1;
	        row_col[1] = 0;
	        int i = 0;
	        boolean done = false;
	        do
	        {
	            if(done)
	                break;
	            for(int j = 0; j < cost[i].length; j++)
	                if(cost[i][j] == 0.0D && rowCover[i] == 0 && colCover[j] == 0)
	                {
	                    row_col[0] = i;
	                    row_col[1] = j;
	                    done = true;
	                }

	            i++;
	            if(i >= cost.length)
	                done = true;
	        } while(true);
	        return row_col;
	    }

	    public static int hg_step5(int step, int[][] mask, int[] rowCover, int[] colCover, int[] zero_RC)
	    {
	        int count = 0;
	        int[][] path = new int[mask[0].length * mask.length][2];
	        path[count][0] = zero_RC[0];
	        path[count][1] = zero_RC[1];
	        boolean done = false;
	        do
	        {
	            if(done)
	                break;
	            int r = findStarInCol(mask, path[count][1]);
	            if(r >= 0)
	            {
	                count++;
	                path[count][0] = r;
	                path[count][1] = path[count - 1][1];
	            } else
	            {
	                done = true;
	            }
	            if(!done)
	            {
	                int c = findPrimeInRow(mask, path[count][0]);
	                count++;
	                path[count][0] = path[count - 1][0];
	                path[count][1] = c;
	            }
	        } while(true);
	        convertPath(mask, path, count);
	        clearCovers(rowCover, colCover);
	        erasePrimes(mask);
	        step = 3;
	        return step;
	    }

	    public static int findStarInCol(int[][] mask, int col)
	    {
	        int r = -1;
	        for(int i = 0; i < mask.length; i++)
	            if(mask[i][col] == 1)
	                r = i;

	        return r;
	    }

	    public static int findPrimeInRow(int[][] mask, int row)
	    {
	        int c = -1;
	        for(int j = 0; j < mask[row].length; j++)
	            if(mask[row][j] == 2)
	                c = j;

	        return c;
	    }

	    public static void convertPath(int[][] mask, int[][] path, int count)
	    {
	        for(int i = 0; i <= count; i++)
	            if(mask[path[i][0]][path[i][1]] == 1)
	                mask[path[i][0]][path[i][1]] = 0;
	            else
	                mask[path[i][0]][path[i][1]] = 1;

	    }

	    public static void erasePrimes(int[][] mask)
	    {
	        for(int i = 0; i < mask.length; i++)
	        {
	            for(int j = 0; j < mask[i].length; j++)
	                if(mask[i][j] == 2)
	                    mask[i][j] = 0;

	        }

	    }

	    public static void clearCovers(int[] rowCover, int[] colCover)
	    {
	        for(int i = 0; i < rowCover.length; i++)
	            rowCover[i] = 0;

	        for(int j = 0; j < colCover.length; j++)
	            colCover[j] = 0;

	    }

	    public static int hg_step6(int step, double[][] cost, int[] rowCover, int[] colCover, double maxCost)
	    {
	        double minval = findSmallest(cost, rowCover, colCover, maxCost);
	        for(int i = 0; i < rowCover.length; i++)
	        {
	            for(int j = 0; j < colCover.length; j++)
	            {
	                if(rowCover[i] == 1)
	                    cost[i][j] = cost[i][j] + minval;
	                if(colCover[j] == 0)
	                    cost[i][j] = cost[i][j] - minval;
	            }

	        }

	        step = 4;
	        return step;
	    }

	    public static double findSmallest(double[][] cost, int[] rowCover, int[] colCover, double maxCost)
	    {
	        double minval = maxCost;
	        for(int i = 0; i < cost.length; i++)
	        {
	            for(int j = 0; j < cost[i].length; j++)
	                if(rowCover[i] == 0 && colCover[j] == 0 && minval > cost[i][j])
	                    minval = cost[i][j];

	        }

	        return minval;
	    }
}