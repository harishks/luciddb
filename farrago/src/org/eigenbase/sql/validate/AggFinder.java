/*
// $Id$
// Package org.eigenbase is a class library of data management components.
// Copyright (C) 2004-2005 The Eigenbase Project
// Copyright (C) 2004-2005 Disruptive Tech
// Copyright (C) 2005-2005 Red Square, Inc.
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.eigenbase.sql.validate;

import org.eigenbase.sql.util.SqlBasicVisitor;
import org.eigenbase.sql.SqlNode;
import org.eigenbase.sql.SqlCall;
import org.eigenbase.sql.SqlKind;
import org.eigenbase.util.Util;

/**
 * Visitor which looks for an aggregate function inside a tree of
 * {@link SqlNode} objects.
 */
class AggFinder extends SqlBasicVisitor
{
    AggFinder()
    {
    }

    public SqlNode findAgg(SqlNode node)
    {
        try {
            node.accept(this);
            return null;
        } catch (Util.FoundOne e) {
            Util.swallow(e, null);
            return (SqlNode) e.getNode();
        }
    }

    public void visit(SqlCall call)
    {
        if (call.operator.isAggregator()) {
            throw new Util.FoundOne(call);
        }
        if (call.isA(SqlKind.Query)) {
            // don't traverse into queries
            return;
        }
        if (call.isA(SqlKind.Over)) {
            // an aggregate function over a window is not an aggregate!
            return;
        }
        super.visit(call);
    }
}