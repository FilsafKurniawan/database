/**

Copyright (C) SYSTAP, LLC 2006-2007.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
/*
 * Created on Apr 13, 2007
 */

package com.bigdata.rdf.inf;

import org.openrdf.model.URI;
import org.openrdf.vocabulary.RDFS;

import com.bigdata.rdf.model.OptimizedValueFactory._URI;
import com.bigdata.rdf.store.AbstractTripleStore;

/**
 * Note: rdfs 5 and 11 use the same base class.
 * 
 * @see RuleRdfs05
 * @see RuleRdfs11
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id$
 */
public class TestRuleRdfs11 extends AbstractRuleTestCase {

    /**
     * 
     */
    public TestRuleRdfs11() {
    }

    /**
     * @param name
     */
    public TestRuleRdfs11(String name) {
        super(name);
    }

    /**
     * Simple test verifies inference of a subclassof entailment.
     */
    public void test_rdfs11() {

        AbstractTripleStore store = getStore();
        
        try {
        
            InferenceEngine inf = new InferenceEngine(store);

            URI A = new _URI("http://www.foo.org/A");
            URI B = new _URI("http://www.foo.org/B");
            URI C = new _URI("http://www.foo.org/C");

            URI rdfsSubClassOf = new _URI(RDFS.SUBCLASSOF);

            store.addStatement(A, rdfsSubClassOf, B);
            store.addStatement(B, rdfsSubClassOf, C);

            assertTrue(store.hasStatement(A, rdfsSubClassOf, B));
            assertTrue(store.hasStatement(B, rdfsSubClassOf, C));
            assertFalse(store.hasStatement(A, rdfsSubClassOf, C));

            applyRule(inf, inf.rdfs11, 1/* numComputed */);

            /*
             * validate the state of the primary store.
             */
            assertTrue(store.hasStatement(A, rdfsSubClassOf, B));
            assertTrue(store.hasStatement(B, rdfsSubClassOf, C));
            assertTrue(store.hasStatement(A, rdfsSubClassOf, C));

        } finally {

            store.closeAndDelete();

        }

    }

}
