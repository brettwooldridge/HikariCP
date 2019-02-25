package com.zaxxer.hikari.mocks;

import com.zaxxer.hikari.metrics.PoolStats;

public class StubPoolStats extends PoolStats
{

   public StubPoolStats(long timeoutMs)
   {
      super(timeoutMs);
   }

   @Override
   protected void update()
   {
      // Do nothing
   }


}
