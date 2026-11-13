package com.zhuhuibin.hypermusiccover.duo;
import org.junit.Test;
import static org.junit.Assert.*;
public class ResourceIdsTest {
 @Test public void repeatedFramesReusePositiveIdsButNeverCrossResourceOwners() {
  ResourceIds.Cache<Object> cache = new ResourceIds.Cache<>();
  Object first = new Object(), second = new Object(); int[] calls = {0};
  for (int i=0;i<1000;i++) assertEquals(7, cache.get(first,"clock",(r,n)->{calls[0]++;return 7;}));
  assertEquals(1,calls[0]);
  assertEquals(8,cache.get(second,"clock",(r,n)->8));
  assertEquals(9,cache.get(first,"date",(r,n)->9));
 }
 @Test public void missingAssetsCanBecomeAvailableAfterFirstFrame() {
  ResourceIds.Cache<Object> cache = new ResourceIds.Cache<>(); Object root = new Object();
  assertEquals(0,cache.get(root,"plugin",(r,n)->0));
  assertEquals(12,cache.get(root,"plugin",(r,n)->12));
  assertEquals(12,cache.get(root,"plugin",(r,n)->99));
 }
}
