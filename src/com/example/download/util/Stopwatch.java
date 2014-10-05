package com.example.download.util;
/**
 * Stop Watch Class
 * Determine time
 * @author GawainGuo
 *
 */
public class Stopwatch
{
   private final long start;
   public Stopwatch()
   {  start = System.currentTimeMillis();  }
   public double elapsedTime()
   {
      long now = System.currentTimeMillis();
      return (now - start);

   }
}