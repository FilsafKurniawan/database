/**

The Notice below must appear in each file of the Source Code of any
copy you distribute of the Licensed Product.  Contributors to any
Modifications may add their own copyright notices to identify their
own contributions.

License:

The contents of this file are subject to the CognitiveWeb Open Source
License Version 1.1 (the License).  You may not copy or use this file,
in either source code or executable form, except in compliance with
the License.  You may obtain a copy of the License from

  http://www.CognitiveWeb.org/legal/license/

Software distributed under the License is distributed on an AS IS
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.  See
the License for the specific language governing rights and limitations
under the License.

Copyrights:

Portions created by or assigned to CognitiveWeb are Copyright
(c) 2003-2003 CognitiveWeb.  All Rights Reserved.  Contact
information for CognitiveWeb is available at

  http://www.CognitiveWeb.org

Portions Copyright (c) 2002-2003 Bryan Thompson.

Acknowledgements:

Special thanks to the developers of the Jabber Open Source License 1.0
(JOSL), from which this License was derived.  This License contains
terms that differ from JOSL.

Special thanks to the CognitiveWeb Open Source Contributors for their
suggestions and support of the Cognitive Web.

Modifications:

*/
/*
 * Created on Nov 27, 2006
 */

package com.bigdata.objndx;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Level;

/**
 * Test suite using {@link IBTree#insert(int, Object)} to split a tree to height
 * two (2) (three levels) and then using {@link IBTree#remove(int)} to reduce
 * the tree back to a single, empty root leaf.
 * 
 * @see src/architecture/btree.xls for the examples used in this test suite.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestSplitJoinThreeLevels extends AbstractBTreeTestCase {

    /**
     * 
     */
    public TestSplitJoinThreeLevels() {
    }

    /**
     * @param name
     */
    public TestSplitJoinThreeLevels(String name) {
        super(name);
    }

    /**
     * Test ability to split and join a tree of order m == 3 driven by the
     * insertion and then the removal of a known sequence of keys. This test
     * checks the state of the tree after each operation against the expected
     * postconditions for that operation. In particular, testing at m == 3 helps
     * to check for fenceposts in the split/join logic.
     * 
     * Note: a branching factor of three (3) is equivilent to a 2-3 tree, where
     * the minimum #of children (for a node) or values (for a leaf) is two (2)
     * and the maximum #of children (for a node) or values (for a leaf) is three
     * (3). This makes it very easy to provoke splits and joins.
     * 
     * There is another version of this test that builds the same tree but uses
     * a different sequence of keys during removal. This provokes some code
     * paths in {@link Node#merge(AbstractNode, boolean)} and
     * {@link Node#redistributeKeys(AbstractNode, boolean)} that are not
     * excercised by this test.
     * 
     * @see #test_splitJoinBranchingFactor3a()
     */
    public void test_splitJoinBranchingFactor3() {

        /*
         * Generate keys, values, and visitation order.
         */
        // keys
        final int[] keys = new int[]{5,6,7,8,3,4,2,1};
        // values
        final SimpleEntry v1 = new SimpleEntry(1);
        final SimpleEntry v2 = new SimpleEntry(2);
        final SimpleEntry v3 = new SimpleEntry(3);
        final SimpleEntry v4 = new SimpleEntry(4);
        final SimpleEntry v5 = new SimpleEntry(5);
        final SimpleEntry v6 = new SimpleEntry(6);
        final SimpleEntry v7 = new SimpleEntry(7);
        final SimpleEntry v8 = new SimpleEntry(8);
        final SimpleEntry[] vals = new SimpleEntry[]{v5,v6,v7,v8,v3,v4,v2,v1};
        // permutation vector for visiting values in key order.
        final int[] order = new int[keys.length];
        // generate visitation order.
        {
            System.arraycopy(keys, 0, order, 0, keys.length);
            Arrays.sort(order);
            System.err.println("keys="+Arrays.toString(keys));
            System.err.println("vals="+Arrays.toString(vals));
            System.err.println("order="+Arrays.toString(order));
        }
        
        final int m = 3;

        BTree btree = getBTree(m);

        assertEquals("height", 0, btree.height);
        assertEquals("#nodes", 0, btree.nnodes);
        assertEquals("#leaves", 1, btree.nleaves);
        assertEquals("#entries", 0, btree.nentries);
        assertTrue(btree.dump(System.err));

        Leaf a = (Leaf) btree.getRoot();
        assertKeys(new int[]{},a);
        assertValues(new Object[]{},a);
        
        int n = 0;
        
        { // insert(5,5)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 5 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            // validate root leaf.
            assertKeys(new int[]{5},a);
            assertValues(new Object[]{v5},a);
            assertTrue(btree.dump(System.err));
        }

        { // insert(6,6)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 6 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            // validate root leaf.
            assertKeys(new int[]{5,6},a);
            assertValues(new Object[]{v5,v6},a);
            assertTrue(btree.dump(System.err));
        }
        
        /*
         * fills the root leaf to capacity.
         * 
         * postcondition:
         * 
         * keys: [ 5 6 7 ]
         */
        { // insert(7,7)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 7 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            // validate root leaf.
            assertKeys(new int[]{5,6,7},a);
            assertValues(new Object[]{v5,v6,v7},a);
            assertTrue(btree.dump(System.err));
        }

        /*
         * splits the root leaf
         * 
         * split(a)->(a,b), c is the new root.
         * 
         * postcondition:
         * 
         * c.keys[ 7 - x ]
         * c.clds[ a b - ]
         * 
         * a.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        final Node c;
        final Leaf b;
        { // insert(8,8)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 8 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            
            // validate new root (c).
            c = (Node)btree.getRoot();
            assertKeys(new int[]{7},c);
            assertEquals(a,c.getChild(0));
            assertNotNull(c.getChild(1));
            assertNull(c.childRefs[2]);
            b = (Leaf)c.getChild(1);
            
            // validate original leaf (a).
            assertKeys(new int[]{5,6},a);
            assertValues(new Object[]{v5,v6},a);
            
            // validate new leaf (b).
            assertKeys(new int[]{7,8},b);
            assertValues(new Object[]{v7,v8},b);
            
            assertTrue(btree.dump(System.err));
        }
        
        /*
         * insert(3,3)
         * 
         * postcondition:
         * 
         * c.keys[ 7 - x ]
         * c.clds[ a b - ]
         * 
         * a.keys[ 3 5 6 ]
         * b.keys[ 7 8 - ]
         */
        {
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 3 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            // validate original leaf (a).
            assertKeys(new int[]{3,5,6},a);
            assertValues(new Object[]{v3,v5,v6},a);
            
        }

        /*
         * insert(4,4), causing split(a)->(a,d) and bringing (c) to capacity.
         * 
         * postcondition:
         * 
         * c.keys[ 5 7 x ]
         * c.clds[ a d b ]
         * 
         * a.keys[ 3 4 - ]
         * d.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        final Leaf d;
        {
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 4 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            
            // validate root (c).
            assertKeys(new int[]{5,7},c);
            assertEquals(a,c.getChild(0));
            assertNotNull(c.childRefs[1]);
            d = (Leaf) c.getChild(1);
            assertEquals(b,c.getChild(2));
            
            // validate original leaf (a).
            assertKeys(new int[]{3,4},a);
            assertValues(new Object[]{v3,v4},a);
            
            // validate new leaf (d).
            assertKeys(new int[]{5,6},d);
            assertValues(new Object[]{v5,v6},d);
            
            // validate leaf (b).
            assertKeys(new int[]{7,8},b);
            assertValues(new Object[]{v7,v8},b);
            
        }
        
        /*
         * insert(2,2), bringing (a) to capacity again.
         */
        {
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 2 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            
            // validate original leaf (a).
            assertKeys(new int[]{2,3,4},a);
            assertValues(new Object[]{v2,v3,v4},a);
            
        }
        
        /*
         * insert(1,1) causing (a) to split(a)->(a,e). Since the root (c) is
         * already at capacity this also causes the root to split(c)->(c,f) and
         * creating a new root(g).
         * 
         * postcondition:
         * 
         * g.keys[ 5 - x ]
         * g.clds[ c f - ]
         * 
         * c.keys[ 3 - x ]
         * c.clds[ a e - ]
         * 
         * f.keys[ 7 - x ]
         * f.clds[ d b - ]
         * 
         * a.keys[ 1 2 - ]
         * e.keys[ 3 4 - ]
         * d.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        final Leaf e;
        final Node f, g;
        {
            
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 1 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));

            // validate the new root(g).
            assertNotSame(c,btree.getRoot());
            g = (Node)btree.getRoot();
            assertKeys(new int[]{5},g);
            assertEquals(c,g.getChild(0));
            assertNotNull(g.childRefs[1]);
            f = (Node) g.getChild(1);
            assertNull(g.childRefs[2]);
            
            // validate old root (c).
            assertKeys(new int[]{3},c);
            assertEquals(a,c.getChild(0));
            assertNotNull(c.childRefs[1]);
            e = (Leaf) c.getChild(1);
            assertNull(c.childRefs[2]);
            
            // validate node(f) split from the old root split(c)->(c,f).
            assertKeys(new int[]{7},f);
            assertEquals(d,f.getChild(0));
            assertEquals(b,f.getChild(1));
            assertNull(f.childRefs[2]);
            
            // validate original leaf (a), which was re-split into (a,e).
            assertKeys(new int[]{1,2},a);
            assertValues(new Object[]{v1,v2},a);
            
            // validate new leaf (e).
            assertKeys(new int[]{3,4},e);
            assertValues(new Object[]{v3,v4},e);
            
            // validate new leaf (d).
            assertKeys(new int[]{5,6},d);
            assertValues(new Object[]{v5,v6},d);
            
            // validate leaf (b).
            assertKeys(new int[]{7,8},b);
            assertValues(new Object[]{v7,v8},b);

        }
        
        /*
         * At this point the tree is setup and we start deleting keys. We delete
         * the keys in (nearly) the reverse order and verify that joins correctly
         * reduce the tree as each node or leaf is reduced below its minimum.
         *  
         * before:
         * 
         * g.keys[ 5 - x ]
         * g.clds[ c f - ]
         * 
         * c.keys[ 3 - x ]
         * c.clds[ a e - ]
         * 
         * f.keys[ 7 - x ]
         * f.clds[ d b - ]
         * 
         * a.keys[ 1 2 - ]
         * e.keys[ 3 4 - ]
         * d.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        assertTrue("before removing keys", btree.dump(Level.DEBUG,System.err));
        
        /*
         * remove(1) triggers a cascade of operations: a.join(e) calls a.merge(e)
         * and c.removeChild(3,e).  This forces c.join(f), which calls c.merge(f)
         * and g.removeChild(5,f).  Since (g) now has a single child we replace
         * the root with (c).  e, f, and g are deleted as we go.
         * 
         * postcondition:
         * 
         * c.keys[ 5 7 x ]
         * c.clds[ a d b ]
         * 
         * a.keys[ 2 3 4 ]
         * d.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         * 
         * e, f, g are deleted.
         */
        assertEquals(v1,btree.remove(1));
        assertTrue("after remove(1)", btree.dump(Level.DEBUG,System.err));
        // verify leaves.
        assertKeys(new int[]{2,3,4},a);
        assertValues(new Object[]{v2,v3,v4},a);
        assertKeys(new int[]{5,6},d);
        assertValues(new Object[]{v5,v6},d);
        assertKeys(new int[]{7,8},b);
        assertValues(new Object[]{v7,v8},b);
        // verify the new root.
        assertKeys(new int[]{5,7},c);
        assertEquals(c,btree.root);
        assertEquals(a,c.getChild(0));
        assertEquals(d,c.getChild(1));
        assertEquals(b,c.getChild(2));
        // verify deleted nodes and leaves.
        assertTrue(e.isDeleted());
        assertTrue(f.isDeleted());
        assertTrue(g.isDeleted());

        /*
         * remove(2) - simple operation just removes(2) from (a).
         */
        assertEquals(v2,btree.remove(2));
        assertTrue("after remove(2)", btree.dump(Level.DEBUG,System.err));
        assertKeys(new int[]{3,4},a);
        assertValues(new Object[]{v3,v4},a);
        
        /*
         * remove(4) triggers a.join(d), which in turn calls a.merge(d) and
         * causes c.removeChild(5,d).
         */
        assertEquals(v4,btree.remove(4));
        assertTrue("after remove(4)", btree.dump(Level.DEBUG,System.err));
        // verify leaves.
        assertKeys(new int[]{3,5,6},a);
        assertValues(new Object[]{v3,v5,v6},a);
        assertKeys(new int[]{7,8},b);
        assertValues(new Object[]{v7,v8},b);
        // verify the root.
        assertKeys(new int[]{7},c);
        assertEquals(c,btree.root);
        assertEquals(a,c.getChild(0));
        assertEquals(b,c.getChild(1));
        assertNull(c.childRefs[2]);
        // verify deleted nodes and leaves.
        assertTrue(d.isDeleted());

        /*
         * remove(8) triggers b.join(a), which in turn calls
         * b.redistributeKeys(a) which sends (6,v6) to (b) and updates the
         * separatorKey in (c) to (6).
         */
        assertEquals(v8,btree.remove(8));
        assertTrue("after remove(8)", btree.dump(Level.DEBUG,System.err));
        // verify leaves.
        assertKeys(new int[]{3,5},a);
        assertValues(new Object[]{v3,v5},a);
        assertKeys(new int[]{6,7},b);
        assertValues(new Object[]{v6,v7},b);
        // verify the root.
        assertKeys(new int[]{6},c);
        assertEquals(c,btree.root);
        assertEquals(a,c.getChild(0));
        assertEquals(b,c.getChild(1));
        assertNull(c.childRefs[2]);

        /*
         * remove(6) triggers b.join(a), which calls b.merge(a) and
         * c.removeChild(-,a). Since (c) now has a single child we replace the
         * root of the tree with (b).
         */
        assertEquals(v6,btree.remove(6));
        assertTrue("after remove(6)", btree.dump(Level.DEBUG,System.err));
        // verify the new root leaf.
        assertKeys(new int[]{3,5,7},b);
        assertValues(new Object[]{v3,v5,v7},b);
        assertEquals(b,btree.root);
        assertTrue(a.isDeleted());
        assertTrue(c.isDeleted());
        
        assertEquals(v7,btree.remove(7));
        assertTrue("after remove(7)", btree.dump(Level.DEBUG,System.err));
        assertKeys(new int[]{3,5},b);
        assertValues(new Object[]{v3,v5},b);
        
        assertEquals(v3,btree.remove(3));
        assertTrue("after remove(3)", btree.dump(Level.DEBUG,System.err));
        assertKeys(new int[]{5},b);
        assertValues(new Object[]{v5},b);
        
        assertEquals(v5,btree.remove(5));
        assertTrue("after remove(5)", btree.dump(Level.DEBUG,System.err));
        assertKeys(new int[]{},b);
        assertValues(new Object[]{},b);
        
        assertEquals("height",0,btree.height);
        assertEquals("nodes",0,btree.nnodes);
        assertEquals("leaves",1,btree.nleaves);
        assertEquals("entries",0,btree.nentries);
        
    }
    
    /**
     * Variant of {@link #test_splitJoinBranchingFactor3()} that excercises some
     * different code paths while removing keys by choosing a different order in
     * which to remove some keys. Both tests build the same initial tree.
     * However, this tests begins by removing a key (7) from the right edge of
     * the tree while the other test beings by removing a key (1) from the left
     * edge of the tree.
     */
    public void test_splitJoinBranchingFactor3b() {

        /*
         * Generate keys, values, and visitation order.
         */
        // keys
        final int[] keys = new int[]{5,6,7,8,3,4,2,1};
        // values
        final SimpleEntry v1 = new SimpleEntry(1);
        final SimpleEntry v2 = new SimpleEntry(2);
        final SimpleEntry v3 = new SimpleEntry(3);
        final SimpleEntry v4 = new SimpleEntry(4);
        final SimpleEntry v5 = new SimpleEntry(5);
        final SimpleEntry v6 = new SimpleEntry(6);
        final SimpleEntry v7 = new SimpleEntry(7);
        final SimpleEntry v8 = new SimpleEntry(8);
        final SimpleEntry[] vals = new SimpleEntry[]{v5,v6,v7,v8,v3,v4,v2,v1};
        // permutation vector for visiting values in key order.
        final int[] order = new int[keys.length];
        // generate visitation order.
        {
            System.arraycopy(keys, 0, order, 0, keys.length);
            Arrays.sort(order);
            System.err.println("keys="+Arrays.toString(keys));
            System.err.println("vals="+Arrays.toString(vals));
            System.err.println("order="+Arrays.toString(order));
        }
        
        final int m = 3;

        BTree btree = getBTree(m);

        assertEquals("height", 0, btree.height);
        assertEquals("#nodes", 0, btree.nnodes);
        assertEquals("#leaves", 1, btree.nleaves);
        assertEquals("#entries", 0, btree.nentries);
        assertTrue(btree.dump(System.err));

        Leaf a = (Leaf) btree.getRoot();
        assertKeys(new int[]{},a);
        assertValues(new Object[]{},a);
        
        int n = 0;
        
        { // insert(5,5)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 5 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            // validate root leaf.
            assertKeys(new int[]{5},a);
            assertValues(new Object[]{v5},a);
            assertTrue(btree.dump(System.err));
        }

        { // insert(6,6)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 6 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            // validate root leaf.
            assertKeys(new int[]{5,6},a);
            assertValues(new Object[]{v5,v6},a);
            assertTrue(btree.dump(System.err));
        }
        
        /*
         * fills the root leaf to capacity.
         * 
         * postcondition:
         * 
         * keys: [ 5 6 7 ]
         */
        { // insert(7,7)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 7 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            // validate root leaf.
            assertKeys(new int[]{5,6,7},a);
            assertValues(new Object[]{v5,v6,v7},a);
            assertTrue(btree.dump(System.err));
        }

        /*
         * splits the root leaf
         * 
         * split(a)->(a,b), c is the new root.
         * 
         * postcondition:
         * 
         * c.keys[ 7 - x ]
         * c.clds[ a b - ]
         * 
         * a.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        final Node c;
        final Leaf b;
        { // insert(8,8)
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 8 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            
            // validate new root (c).
            c = (Node)btree.getRoot();
            assertKeys(new int[]{7},c);
            assertEquals(a,c.getChild(0));
            assertNotNull(c.getChild(1));
            assertNull(c.childRefs[2]);
            b = (Leaf)c.getChild(1);
            
            // validate original leaf (a).
            assertKeys(new int[]{5,6},a);
            assertValues(new Object[]{v5,v6},a);
            
            // validate new leaf (b).
            assertKeys(new int[]{7,8},b);
            assertValues(new Object[]{v7,v8},b);
            
            assertTrue(btree.dump(System.err));
        }
        
        /*
         * insert(3,3)
         * 
         * postcondition:
         * 
         * c.keys[ 7 - x ]
         * c.clds[ a b - ]
         * 
         * a.keys[ 3 5 6 ]
         * b.keys[ 7 8 - ]
         */
        {
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 3 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            // validate original leaf (a).
            assertKeys(new int[]{3,5,6},a);
            assertValues(new Object[]{v3,v5,v6},a);
            
        }

        /*
         * insert(4,4), causing split(a)->(a,d) and bringing (c) to capacity.
         * 
         * postcondition:
         * 
         * c.keys[ 5 7 x ]
         * c.clds[ a d b ]
         * 
         * a.keys[ 3 4 - ]
         * d.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        final Leaf d;
        {
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 4 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            
            // validate root (c).
            assertKeys(new int[]{5,7},c);
            assertEquals(a,c.getChild(0));
            assertNotNull(c.childRefs[1]);
            d = (Leaf) c.getChild(1);
            assertEquals(b,c.getChild(2));
            
            // validate original leaf (a).
            assertKeys(new int[]{3,4},a);
            assertValues(new Object[]{v3,v4},a);
            
            // validate new leaf (d).
            assertKeys(new int[]{5,6},d);
            assertValues(new Object[]{v5,v6},d);
            
            // validate leaf (b).
            assertKeys(new int[]{7,8},b);
            assertValues(new Object[]{v7,v8},b);
            
        }
        
        /*
         * insert(2,2), bringing (a) to capacity again.
         */
        {
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 2 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));
            
            // validate original leaf (a).
            assertKeys(new int[]{2,3,4},a);
            assertValues(new Object[]{v2,v3,v4},a);
            
        }
        
        /*
         * insert(1,1) causing (a) to split(a)->(a,e). Since the root (c) is
         * already at capacity this also causes the root to split(c)->(c,f) and
         * creating a new root(g).
         * 
         * postcondition:
         * 
         * g.keys[ 5 - x ]
         * g.clds[ c f - ]
         * 
         * c.keys[ 3 - x ]
         * c.clds[ a e - ]
         * 
         * f.keys[ 7 - x ]
         * f.clds[ d b - ]
         * 
         * a.keys[ 1 2 - ]
         * e.keys[ 3 4 - ]
         * d.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        final Leaf e;
        final Node f, g;
        {
            
            int key = keys[n];
            SimpleEntry val = vals[n++];
            assert key == 1 && val.id() == key;
            assertNull(btree.remove(key)); // not found / no change.
            assertNull(btree.lookup(key)); // not found.
            assertNull(btree.insert(key,val)); // insert.
            assertEquals(val,btree.lookup(key)); // found.
            assertTrue(btree.dump(Level.DEBUG,System.err));

            // validate the new root(g).
            assertNotSame(c,btree.getRoot());
            g = (Node)btree.getRoot();
            assertKeys(new int[]{5},g);
            assertEquals(c,g.getChild(0));
            assertNotNull(g.childRefs[1]);
            f = (Node) g.getChild(1);
            assertNull(g.childRefs[2]);
            
            // validate old root (c).
            assertKeys(new int[]{3},c);
            assertEquals(a,c.getChild(0));
            assertNotNull(c.childRefs[1]);
            e = (Leaf) c.getChild(1);
            assertNull(c.childRefs[2]);
            
            // validate node(f) split from the old root split(c)->(c,f).
            assertKeys(new int[]{7},f);
            assertEquals(d,f.getChild(0));
            assertEquals(b,f.getChild(1));
            assertNull(f.childRefs[2]);
            
            // validate original leaf (a), which was re-split into (a,e).
            assertKeys(new int[]{1,2},a);
            assertValues(new Object[]{v1,v2},a);
            
            // validate new leaf (e).
            assertKeys(new int[]{3,4},e);
            assertValues(new Object[]{v3,v4},e);
            
            // validate new leaf (d).
            assertKeys(new int[]{5,6},d);
            assertValues(new Object[]{v5,v6},d);
            
            // validate leaf (b).
            assertKeys(new int[]{7,8},b);
            assertValues(new Object[]{v7,v8},b);

        }
        
        /*
         * At this point the tree is setup and we start deleting keys. We delete
         * the keys in (nearly) the reverse order and verify that joins correctly
         * reduce the tree as each node or leaf is reduced below its minimum.
         *  
         * before:
         * 
         * g.keys[ 5 - x ]
         * g.clds[ c f - ]
         * 
         * c.keys[ 3 - x ]
         * c.clds[ a e - ]
         * 
         * f.keys[ 7 - x ]
         * f.clds[ d b - ]
         * 
         * a.keys[ 1 2 - ]
         * e.keys[ 3 4 - ]
         * d.keys[ 5 6 - ]
         * b.keys[ 7 8 - ]
         */
        assertTrue("before removing keys", btree.dump(Level.DEBUG,System.err));
        
        /*
         * remove(7) triggers a cascade of operations: b.join(d) calls b.merge(d)
         * and f.removeChild(-,d).  This forces f.join(c), which calls f.merge(c)
         * and g.removeChild(-,c).  Since (g) now has a single child we replace
         * the root with (f).  d, c, and g are deleted as we go.
         *
         * postcondition:
         * 
         * f.keys[ 3 5 x ]
         * f.clds[ a e b ]
         * 
         * a.keys[ 1 2 - ]
         * e.keys[ 3 4 - ]
         * b.keys[ 5 6 8 ]
         * 
         * c, d, g are deleted.
         */
        assertEquals(v7,btree.remove(7));
        assertTrue("after remove(7)", btree.dump(Level.DEBUG,System.err));
        // verify leaves.
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2},a);
        assertKeys(new int[]{3,4},e);
        assertValues(new Object[]{v3,v4},e);
        assertKeys(new int[]{5,6,8},b);
        assertValues(new Object[]{v5,v6,v8},b);
        // verify the new root.
        assertKeys(new int[]{3,5},f);
        assertEquals(f,btree.root);
        assertEquals(a,f.getChild(0));
        assertEquals(e,f.getChild(1));
        assertEquals(b,f.getChild(2));
        // verify deleted nodes and leaves.
        assertTrue(c.isDeleted());
        assertTrue(d.isDeleted());
        assertTrue(g.isDeleted());

        /*
         * remove(3) triggers e.join(b) which calls e.redistributeKeys(b) and
         * sends (5,v5) to e. (This tests the code path for redistribution of
         * keys with a rightSibling of a leaf).
         * 
         * postcondition:
         * 
         * f.keys[ 3 6 x ]
         * f.clds[ a e b ]
         * 
         * a.keys[ 1 2 - ]
         * e.keys[ 4 5 - ]
         * b.keys[ 6 8 - ]
         */
        assertEquals(v3,btree.remove(3));
        assertTrue("after remove(3)", btree.dump(Level.DEBUG,System.err));
        // verify leaves.
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2},a);
        assertKeys(new int[]{4,5},e);
        assertValues(new Object[]{v4,v5},e);
        assertKeys(new int[]{6,8},b);
        assertValues(new Object[]{v6,v8},b);
        // verify the new root.
        assertKeys(new int[]{3,6},f);
        assertEquals(f,btree.root);
        assertEquals(a,f.getChild(0));
        assertEquals(e,f.getChild(1));
        assertEquals(b,f.getChild(2));

        /*
         * remove(8) triggers b.join(e) which calls b.merge(e), which updates
         * the separator in (f) to the separator for the leftSibling which is
         * (3) and then invokes f.removeChild(e).
         * 
         * postcondition:
         * 
         * f.keys[ 3 - x ]
         * f.clds[ a b - ]
         * 
         * a.keys[ 1 2 - ]
         * b.keys[ 4 5 6 ]
         * 
         * e is deleted.
         */
        assertEquals(v8,btree.remove(8));
        assertTrue("after remove(8)", btree.dump(Level.DEBUG,System.err));
        // verify leaves.
        assertKeys(new int[]{1,2},a);
        assertValues(new Object[]{v1,v2},a);
        assertKeys(new int[]{4,5,6},b);
        assertValues(new Object[]{v4,v5,v6},b);
        // verify the root.
        assertKeys(new int[]{3},f);
        assertEquals(f,btree.root);
        assertEquals(a,f.getChild(0));
        assertEquals(b,f.getChild(1));
        assertNull(f.childRefs[2]);
        assertTrue(e.isDeleted());

        /*
         * remove(2) triggers a.join(b) which triggers a.redistributeKeys(b)
         * which sends (4,v4) to (a) and updates the separatorKey on (f) to
         * (5).
         * 
         * postcondition:
         * 
         * f.keys[ 3 - x ]
         * f.clds[ a b - ]
         * 
         * a.keys[ 1 4 - ]
         * b.keys[ 5 6 - ]
         */
        assertEquals(v2,btree.remove(2));
        assertTrue("after remove(2)", btree.dump(Level.DEBUG,System.err));
        // verify leaves.
        assertKeys(new int[]{1,4},a);
        assertValues(new Object[]{v1,v4},a);
        assertKeys(new int[]{5,6},b);
        assertValues(new Object[]{v5,v6},b);
        // verify the root.
        assertKeys(new int[]{5},f);
        assertEquals(f,btree.root);
        assertEquals(a,f.getChild(0));
        assertEquals(b,f.getChild(1));
        assertNull(f.childRefs[2]);
        
        /*
         * remove(1) triggers a.join(b) which calls a.merge(b) and
         * f.removeChild(b). Since this leaves (f) with only one child, we make
         * (a) the new root of the tree.
         * 
         * postcondition:
         * 
         * a.keys[ 4 5 6 ]
         * 
         * b, f is deleted.
         */
        assertEquals(v1,btree.remove(1));
        assertTrue("after remove(1)", btree.dump(Level.DEBUG,System.err));
        // verify the remaining leaf, which is now the root of the tree.
        assertKeys(new int[]{4,5,6},a);
        assertValues(new Object[]{v4,v5,v6},a);
        assertEquals(a,btree.root);
        assertTrue(b.isDeleted());
        assertTrue(f.isDeleted());
        
        /*
         * At this point we have only the root leaf and we just delete the final
         * keys.
         */
        assertEquals(v4,btree.remove(4));
        assertTrue("after remove(4)", btree.dump(Level.DEBUG,System.err));
        assertKeys(new int[]{5,6},a);
        assertValues(new Object[]{v5,v6},a);
        assertEquals(a,btree.root);

        assertEquals(v6,btree.remove(6));
        assertTrue("after remove(6)", btree.dump(Level.DEBUG,System.err));
        assertKeys(new int[]{5},a);
        assertValues(new Object[]{v5},a);
        assertEquals(a,btree.root);

        assertEquals(v5,btree.remove(5));
        assertTrue("after remove(5)", btree.dump(Level.DEBUG,System.err));
        assertKeys(new int[]{},a);
        assertValues(new Object[]{},a);
        assertEquals(a,btree.root);

        assertEquals("height",0,btree.height);
        assertEquals("nodes",0,btree.nnodes);
        assertEquals("leaves",1,btree.nleaves);
        assertEquals("entries",0,btree.nentries);

    }
    
    /**
     * <p>
     * Test ability to split the root leaf. A Btree is created with a known
     * capacity. The root leaf is filled to capacity and then split. The keys
     * are choosen so as to create room for an insert into the left and right
     * leaves after the split. The state of the root leaf before the split is:
     * </p>
     * 
     * <pre>
     *    root keys : [ 1 11 21 31 ]
     * </pre>
     * 
     * <p>
     * The root leaf is split by inserting the external key <code>15</code>.
     * The state of the tree after the split is:
     * </p>
     * 
     * <pre>
     *    m     = 4 (branching factor)
     *    m/2   = 2 (index of first key moved to the new leaf)
     *    m/2-1 = 1 (index of last key retained in the old leaf).
     *               
     *    root  keys : [ 21 ]
     *    leaf1 keys : [  1 11 15  - ]
     *    leaf2 keys : [ 21 31  -  - ]
     * </pre>
     * 
     * <p>
     * The test then inserts <code>2</code> (goes into leaf1, filling it to
     * capacity), <code>22</code> (goes into leaf2, testing the edge condition
     * for inserting the key greater than the split key), and <code>24</code>
     * (goes into leaf2, filling it to capacity). At this point the tree looks
     * like this:
     * </p>
     * 
     * <pre>
     *    root  keys : [ 21 ]
     *    leaf1 keys : [  1  2 11 15 ]
     *    leaf2 keys : [ 21 22 24 31 ]
     * </pre>
     * 
     * <p>
     * The test now inserts <code>7</code>, causing leaf1 into split (note
     * that the leaves are named by their creation order, not their traveral
     * order):
     * </p>
     * 
     * <pre>
     *    root  keys : [ 11 21 ]
     *    leaf1 keys : [  1  2  7  - ]
     *    leaf3 keys : [ 11 15  -  - ]
     *    leaf2 keys : [ 21 22 24 31 ]
     * </pre>
     * 
     * <p>
     * The test now inserts <code>23</code>, causing leaf2 into split:
     * </p>
     * 
     * <pre>
     *    root  keys : [ 11 21 24 ]
     *    leaf1 keys : [  1  2  7  - ]
     *    leaf3 keys : [ 11 15  -  - ]
     *    leaf2 keys : [ 21 22 23  - ]
     *    leaf4 keys : [ 24 31  -  - ]
     * </pre>
     * 
     * <p>
     * At this point the root node is at capacity and another split of a leaf
     * will cause the root node to split and increase the height of the btree.
     * To prepare for this, we insert <4> (into leaf1), <code>17</code> and
     * <code>18</code> (into leaf3), and <code>35</code> and <code>40</code>
     * (into leaf4). This gives us the following scenario.
     * </p>
     * 
     * <pre>
     *    root  keys : [ 11 21 24 ]
     *    leaf1 keys : [  1  2  4  7 ]
     *    leaf3 keys : [ 11 15 17 18 ]
     *    leaf2 keys : [ 21 22 23  - ]
     *    leaf4 keys : [ 24 31 35 40 ]
     * </pre>
     * 
     * <p>
     * Note that leaf2 has a hole that can not be filled by an insert since the
     * key <code>24</code> is already in leaf4.
     * </p>
     * <p>
     * Now we insert <code>50</code> into leaf4, forcing leaf4 to split, which
     * in turn requires the root node to split. The result is as follows (note
     * that the old root is now named 'node1' and 'node2' is the new non-root
     * node created by the split of the old root node). The split is not made
     * until the insert reaches the leaf, discovers that the key is not already
     * present, and then discovers that the leaf is full. The key does into the
     * new leaf, leaf5.
     * </p>
     * 
     * <pre>
     *    root  keys : [ 21  -  - ]
     *    node1 keys : [ 11  -  - ]
     *    node2 keys : [ 24 40  - ]
     *    leaf1 keys : [  1  2  4  7 ]
     *    leaf3 keys : [ 11 15 17 18 ]
     *    leaf2 keys : [ 21 22 23  - ]
     *    leaf4 keys : [ 24 31 35  - ]
     *    leaf5 keys : [ 40 50  -  - ]
     * </pre>
     * 
     * FIXME This example needs to be competely reworked since it relied on an
     *       incorrect split rule. When doing so, be sure to use two variants so
     *       that we can test out Node#redistributeKeys with a left and a right
     *       sibling since it is not possible to test those code paths with a
     *       tree of order less than 4.
     */
    public void test_splitJoinBranchingFactor4() {

        fail("re-write this test");
        
        final int m = 4;

        BTree btree = getBTree(m);

        assertEquals("height", 0, btree.height);
        assertEquals("#nodes", 0, btree.nnodes);
        assertEquals("#leaves", 1, btree.nleaves);
        assertEquals("#entries", 0, btree.nentries);
        
        Leaf leaf1 = (Leaf) btree.getRoot();
        
        int[] keys = new int[]{1,11,21,31};
        
        for( int i=0; i<m; i++ ) {
         
            int key = keys[i];
            
            SimpleEntry entry = new SimpleEntry();
            
            assertNull(btree.lookup(key));
            
            btree.insert(key, entry);
            
            assertEquals(entry,btree.lookup(key));
            
        }

        // Verify leaf is full.
        assertEquals( m, leaf1.nkeys );
        
        // Verify keys.
        assertEquals( keys, leaf1.keys );
        
        // Verify root node has not been changed.
        assertEquals( leaf1, btree.getRoot() );

        /*
         * split the root leaf.
         */

        // Insert [key := 15] goes into leaf1 (forces split).
        System.err.print("leaf1 before split : "); leaf1.dump(System.err);
        SimpleEntry splitEntry = new SimpleEntry();
        assertNull(btree.lookup(15));
        btree.insert(15,splitEntry);
        System.err.print("leaf1 after split : "); leaf1.dump(System.err);
        assertEquals("leaf1.nkeys",3,leaf1.nkeys);
        assertEquals("leaf1.keys",new int[]{1,11,15,0},leaf1.keys);
        assertEquals(splitEntry,btree.lookup(15));

        assertEquals("height", 1, btree.height);
        assertEquals("#nodes", 1, btree.nnodes);
        assertEquals("#leaves", 2, btree.nleaves);
        assertEquals("#entries", 5, btree.nentries);

        /*
         * Verify things about the new root node.
         */
        assertTrue( btree.getRoot() instanceof Node );
        Node root = (Node) btree.getRoot();
        System.err.print("root after split : "); root.dump(System.err);
        assertEquals("root.nkeys",1,root.nkeys);
        assertEquals("root.keys",new int[]{21,0,0},root.keys);
        assertEquals(leaf1,root.getChild(0));
        assertNotNull(root.getChild(1));
        assertNotSame(leaf1,root.getChild(1));

        /*
         * Verify things about the new leaf node, which we need to access
         * from the new root node.
         */
        Leaf leaf2 = (Leaf)root.getChild(1);
        System.err.print("leaf2 after split : "); leaf2.dump(System.err);
        assertEquals("leaf2.nkeys",m/2,leaf2.nkeys);
        assertEquals("leaf2.keys",new int[]{21,31,0,0},leaf2.keys);

        /*
         * verify iterator.
         */
        assertSameIterator(new AbstractNode[] { leaf1, leaf2, root }, root
                .postOrderIterator());

        // Insert [key := 2] goes into leaf1, filling it to capacity.
        btree.insert(2,new SimpleEntry());
        assertEquals("leaf1.nkeys",4,leaf1.nkeys);
        assertEquals("leaf1.keys",new int[]{1,2,11,15},leaf1.keys);

        // Insert [key := 22] goes into leaf2 (tests edge condition)
        btree.insert(22,new SimpleEntry());
        assertEquals("leaf2.nkeys",3,leaf2.nkeys);
        assertEquals("leaf2.keys",new int[]{21,22,31,0},leaf2.keys);

        // Insert [key := 24] goes into leaf2, filling it to capacity.
        btree.insert(24,new SimpleEntry());
        assertEquals("leaf2.nkeys",4,leaf2.nkeys);
        assertEquals("leaf2.keys",new int[]{21,22,24,31},leaf2.keys);

//        System.err.print("root  final : "); root.dump(System.err);
//        System.err.print("leaf1 final : "); leaf1.dump(System.err);
//        System.err.print("leaf2 final : "); leaf2.dump(System.err);
        
        assertEquals("height", 1, btree.height);
        assertEquals("#nodes", 1, btree.nnodes);
        assertEquals("#leaves", 2, btree.nleaves);
        assertEquals("#entries", 8, btree.nentries);

        /*
         * Insert into leaf1, causing it to split. The split will cause a new
         * child to be added to the root. Verify the post-conditions.
         */
        
        // Insert [key := 7] goes into leaf1, forcing split.
        assertEquals("leaf1.nkeys",m,leaf1.nkeys);
        System.err.print("root  before split: ");root.dump(System.err);
        System.err.print("leaf1 before split: ");leaf1.dump(System.err);
        btree.insert(7,new SimpleEntry());
        System.err.print("root  after split: ");root.dump(System.err);
        System.err.print("leaf1 after split: ");leaf1.dump(System.err);
        assertEquals("leaf1.nkeys",3,leaf1.nkeys);
        assertEquals("leaf1.keys",new int[]{1,2,7,0},leaf1.keys);

        assertEquals("root.nkeys",2,root.nkeys);
        assertEquals("root.keys",new int[]{11,21,0},root.keys);
        assertEquals(leaf1,root.getChild(0));
        assertEquals(leaf2,root.getChild(2));

        Leaf leaf3 = (Leaf)root.getChild(1);
        assertNotNull( leaf3 );
        assertEquals("leaf3.nkeys",2,leaf3.nkeys);
        assertEquals("leaf3.keys",new int[]{11,15,0,0},leaf3.keys);

        assertEquals("height", 1, btree.height);
        assertEquals("#nodes", 1, btree.nnodes);
        assertEquals("#leaves", 3, btree.nleaves);
        assertEquals("#entries", 9, btree.nentries);

        /*
         * verify iterator.
         */
        assertSameIterator(new AbstractNode[] { leaf1, leaf3, leaf2, root },
                root.postOrderIterator());

        /*
         * Insert into leaf2, causing it to split. The split will cause a new
         * child to be added to the root. At this point the root node is at
         * capacity.  Verify the post-conditions.
         */
        
        // Insert [key := 23] goes into leaf2, forcing split.
        assertEquals("leaf2.nkeys",m,leaf2.nkeys);
        System.err.print("root  before split: ");root.dump(System.err);
        System.err.print("leaf2 before split: ");leaf2.dump(System.err);
        btree.insert(23,new SimpleEntry());
        System.err.print("root  after split: ");root.dump(System.err);
        System.err.print("leaf2 after split: ");leaf2.dump(System.err);
        assertEquals("leaf2.nkeys",3,leaf2.nkeys);
        assertEquals("leaf2.keys",new int[]{21,22,23,0},leaf2.keys);

        assertEquals("root.nkeys",3,root.nkeys);
        assertEquals("root.keys",new int[]{11,21,24},root.keys);
        assertEquals(leaf1,root.getChild(0));
        assertEquals(leaf3,root.getChild(1));
        assertEquals(leaf2,root.getChild(2));

        Leaf leaf4 = (Leaf)root.getChild(3);
        assertNotNull( leaf4 );
        assertEquals("leaf4.nkeys",2,leaf4.nkeys);
        assertEquals("leaf4.keys",new int[]{24,31,0,0},leaf4.keys);

        assertEquals("height", 1, btree.height);
        assertEquals("#nodes", 1, btree.nnodes);
        assertEquals("#leaves", 4, btree.nleaves);
        assertEquals("#entries", 10, btree.nentries);

        /*
         * verify iterator.
         */
        assertSameIterator(new AbstractNode[] { leaf1, leaf3, leaf2, leaf4,
                root }, root.postOrderIterator());

        /*
         * At this point the root node is at capacity and another split of a
         * leaf will cause the root node to split and increase the height of the
         * btree. We prepare for that scenario now by filling up a few of the
         * leaves to capacity.
         */
        
        // Save a reference to the root node before the split.
        Node node1 = (Node) btree.root;
        assertEquals(root,node1);

        // Insert [key := 4] into leaf1, filling it to capacity.
        btree.insert(4,new SimpleEntry());
        assertEquals("leaf1.nkeys",4,leaf1.nkeys);
        assertEquals("leaf1.keys",new int[]{1,2,4,7},leaf1.keys);

        // Insert [key := 17] into leaf3.
        btree.insert(17,new SimpleEntry());
        assertEquals("leaf3.nkeys",3,leaf3.nkeys);
        assertEquals("leaf3.keys",new int[]{11,15,17,0},leaf3.keys);

        // Insert [key := 18] into leaf3, filling it to capacity.
        btree.insert(18,new SimpleEntry());
        assertEquals("leaf3.nkeys",4,leaf3.nkeys);
        assertEquals("leaf3.keys",new int[]{11,15,17,18},leaf3.keys);

        // Insert [key := 35] into leaf4.
        btree.insert(35,new SimpleEntry());
        assertEquals("leaf4.nkeys",3,leaf4.nkeys);
        assertEquals("leaf4.keys",new int[]{24,31,35,0},leaf4.keys);

        // Insert [key := 40] into leaf4, filling it to capacity.
        btree.insert(40,new SimpleEntry());
        assertEquals("leaf4.nkeys",4,leaf4.nkeys);
        assertEquals("leaf4.keys",new int[]{24,31,35,40},leaf4.keys);

        assertEquals("height", 1, btree.height);
        assertEquals("#nodes", 1, btree.nnodes);
        assertEquals("#leaves", 4, btree.nleaves);
        assertEquals("#entries", 15, btree.nentries);
        
        /*
         * verify iterator (no change).
         */
        assertSameIterator(new AbstractNode[] { leaf1, leaf3, leaf2, leaf4,
                root }, root.postOrderIterator());

        /*
         * Force leaf4 to split.
         */
        
        System.err.print("Tree pre-split");
        assertTrue(btree.dump(System.err));
        
        // Insert [key := 50] into leaf4, forcing it to split.  The insert
        // goes into the _new_ leaf.
        leaf4.dump(System.err);
        leaf4.getParent().dump(System.err);
        btree.insert(50,new SimpleEntry());
        leaf4.dump(System.err);
        leaf4.getParent().dump(System.err);

        /*
         * verify keys the entire tree, starting at the new root.
         */

        System.err.print("Tree post-split");
        assertTrue(btree.dump(System.err));
        
        assertNotSame(root,btree.root); // verify new root.
        root = (Node)btree.root;
        assertEquals("root.nkeys",1,root.nkeys);
        assertEquals("root.keys",new int[]{21,0,0},root.keys);
        assertEquals(node1,root.getChild(0));
        assertNotNull(root.getChild(1));
        Node node2 = (Node)root.getChild(1);
        
        assertEquals("node1.nkeys",1,node1.nkeys);
        assertEquals("node1.keys",new int[]{11,0,0},node1.keys);
        assertEquals(root,node1.getParent());
        assertEquals(leaf1,node1.getChild(0));
        assertEquals(leaf3,node1.getChild(1));

        assertEquals("leaf1.nkeys",4,leaf1.nkeys);
        assertEquals("leaf1.keys",new int[]{1,2,4,7},leaf1.keys);
        assertEquals(node1,leaf1.getParent());

        assertEquals("leaf3.nkeys",4,leaf3.nkeys);
        assertEquals("leaf3.keys",new int[]{11,15,17,18},leaf3.keys);
        assertEquals(node1,leaf3.getParent());
        
        assertEquals("node2.nkeys",2,node2.nkeys);
        assertEquals("node2.keys",new int[]{24,40,0},node2.keys);
        assertEquals(root,node2.getParent());
        assertEquals(leaf2,node2.getChild(0));
        assertEquals(leaf4,node2.getChild(1));
        assertNotNull(node2.getChild(2));
        Leaf leaf5 = (Leaf)node2.getChild(2);

        assertEquals("leaf2.nkeys",3,leaf2.nkeys);
        assertEquals("leaf2.keys",new int[]{21,22,23,0},leaf2.keys);
        assertEquals(node2,leaf2.getParent());

        assertEquals("leaf4.nkeys",3,leaf4.nkeys);
        assertEquals("leaf4.keys",new int[]{24,31,35,0},leaf4.keys);
        assertEquals(node2,leaf4.getParent());
        
        assertEquals("leaf5.nkeys",2,leaf5.nkeys);
        assertEquals("leaf5.keys",new int[]{40,50,0,0},leaf5.keys);
        assertEquals(node2,leaf5.getParent());

        assertEquals("height", 2, btree.height);
        assertEquals("#nodes", 3, btree.nnodes);
        assertEquals("#leaves", 5, btree.nleaves);
        assertEquals("#entries", 16, btree.nentries);

        /*
         * verify iterator.
         */
        assertSameIterator(new AbstractNode[] { leaf1, leaf3, node1, leaf2, 
                leaf4, leaf5, node2, root }, root.postOrderIterator());
     
        fail("Delete keys reducing the tree back to an empty tree.");

    }
    
}
