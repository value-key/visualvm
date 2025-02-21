/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.jfr.views.sampler;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.views.components.MessageComponent;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.ui.Formatters;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.NumberPercentRenderer;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class MemorySamplerViewSupport {
    
    static final class HeapViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean hasData;
        
        private Map<String, Long[]> eventData;
        
        private String[] names;
        private long[] sizes;
        private long[] counts;
        
        private HeapTableModel tableModel;
        private ProfilerTable table;
        private HideableBarRenderer[] renderers;
        
        
        HeapViewSupport(JFRModel model) {
            hasData = model.containsEvent(JFRSnapshotSamplerViewProvider.ObjectCountChecker.class);
            
            initComponents();
        }
        
        
        @Override
        public void init() {
            if (hasData) eventData = new HashMap();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (!hasData) return true;
            
            if (JFRSnapshotSamplerViewProvider.EVENT_OBJECT_COUNT.equals(typeName)) {
                try {
                    eventData.put(event.getClass("objectClass").getName(), new Long[] { event.getLong("totalSize"), event.getLong("count") }); // NOI18N
                } catch (JFRPropertyNotAvailableException e) {
                    System.err.println(">>> " + e);
                }
            }
            return false;
        }

        @Override
        public void done() {
            if (hasData) SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    long total1 = 0, total2 = 0;
                    long max1 = 0, max2 = 0;
                    
                    names = new String[eventData.size()];
                    sizes = new long[eventData.size()];
                    counts = new long[eventData.size()];

                    int i = 0;
                    for (Map.Entry<String, Long[]> entry : eventData.entrySet()) {
                        names[i] = decodeClassName(entry.getKey());
                        sizes[i] = entry.getValue()[0];
                        counts[i] = entry.getValue()[1];
                        max1 = sizes[i] > max1 ? sizes[i] : max1;
                        total1 += sizes[i];
                        max2 = counts[i] > max2 ? counts[i] : max2;
                        total2 += counts[i];
                        i++;
                    }
                    
                    renderers[0].setMaxValue(max1);
                    table.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
                    renderers[0].setMaxValue(total1);
                    
                    renderers[1].setMaxValue(max2);
                    table.setDefaultColumnWidth(2, renderers[1].getOptimalWidth());
                    renderers[1].setMaxValue(total2);
                    
                    tableModel.fireTableDataChanged();

                    eventData.clear();
                    eventData = null;
                }
            });
        }
        
        private static String decodeClassName(String className) {
            className = StringUtils.userFormClassName(className);
            
            if (className.startsWith("L") && className.contains(";")) // NOI18N
                className = className.substring(1, className.length()).replace(";", ""); // NOI18N
            
            return className;
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerViewSupport.class, "LBL_Heap_histogram"), null, 10, this, null); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (!hasData) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("Heap histogram", JFRSnapshotSamplerViewProvider.ObjectCountChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                tableModel = new HeapTableModel();
                table = new ProfilerTable(tableModel, true, true, null);

                table.setMainColumn(0);
                table.setFitWidthColumn(0);

                table.setSortColumn(1);
                table.setDefaultSortOrder(SortOrder.DESCENDING);
                table.setDefaultSortOrder(0, SortOrder.ASCENDING);

                renderers = new HideableBarRenderer[2];
                renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));
                renderers[1] = new HideableBarRenderer(new NumberPercentRenderer());

                JavaNameRenderer classRenderer = new JavaNameRenderer(Icons.getIcon(LanguageIcons.CLASS));

                table.setColumnRenderer(0, classRenderer);
                table.setColumnRenderer(1, renderers[0]);
                table.setColumnRenderer(2, renderers[1]);

                add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
            }
        }
        
        
        private class HeapTableModel extends AbstractTableModel {
        
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Name";
                } else if (columnIndex == 1) {
                    return "Bytes";
                } else if (columnIndex == 2) {
                    return "Objects";
                }

                return null;
            }

            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return String.class;
                } else {
                    return Long.class;
                }
            }

            public int getRowCount() {
                return names == null ? 0 : names.length;
            }

            public int getColumnCount() {
                return 3;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return names[rowIndex];
                } else if (columnIndex == 1) {
                    return sizes[rowIndex];
                } else if (columnIndex == 2) {
                    return counts[rowIndex];
                }

                return null;
            }

        }
        
    }
    
    
    static final class ThreadsMemoryViewSupport extends JPanel implements JFREventVisitor {
        
        private final boolean hasData;
        
        private Map<String, Long> eventData;
        
        private String[] names;
        private long[] values;
        
        private TreadsAllocTableModel tableModel;
        private ProfilerTable table;
        private HideableBarRenderer[] renderers;
        
        
        ThreadsMemoryViewSupport(JFRModel model) {
            hasData = model.containsEvent(JFRSnapshotSamplerViewProvider.ThreadAllocationsChecker.class);
            
            initComponents();
        }
        
        
        @Override
        public void init() {
            if (hasData) eventData = new HashMap();
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (!hasData) return true;
            
            if (JFRSnapshotSamplerViewProvider.EVENT_THREAD_ALLOCATIONS.equals(typeName)) { // NOI18N
                try {
                    String threadName = event.getThread("eventThread").getName(); // NOI18N
                    long allocated = event.getLong("allocated"); // NOI18N
                    Long _allocated = eventData.get(threadName);
                    if (_allocated == null || _allocated < allocated)
                        eventData.put(threadName, allocated);
                } catch (JFRPropertyNotAvailableException e) {}
            }
            return false;
        }

        @Override
        public void done() {
            if (hasData) SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    long total = 0;
                    long max = 0;
                    
                    names = new String[eventData.size()];
                    values = new long[eventData.size()];

                    int i = 0;
                    for (Map.Entry<String, Long> entry : eventData.entrySet()) {
                        names[i] = entry.getKey();
                        values[i] = entry.getValue();
                        max = values[i] > max ? values[i] : max;
                        total += values[i++];
                    }
                    
                    renderers[0].setMaxValue(max);
                    table.setDefaultColumnWidth(1, renderers[0].getOptimalWidth());
                    renderers[0].setMaxValue(total);
                    
                    tableModel.fireTableDataChanged();

                    eventData.clear();
                    eventData = null;
                }
            });
        }
        
        
        DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(NbBundle.getMessage(
                CPUSamplerViewSupport.class, "LBL_ThreadAlloc_M"), null, 20, this, null); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            setOpaque(false);
            
            if (!hasData) {
                setLayout(new BorderLayout());
                add(MessageComponent.noData("Per thread allocations", JFRSnapshotSamplerViewProvider.ThreadAllocationsChecker.checkedTypes()), BorderLayout.CENTER);
            } else {
                tableModel = new TreadsAllocTableModel();
                table = new ProfilerTable(tableModel, true, true, null);

                table.setMainColumn(0);
                table.setFitWidthColumn(0);

                table.setSortColumn(1);
                table.setDefaultSortOrder(SortOrder.DESCENDING);
                table.setDefaultSortOrder(0, SortOrder.ASCENDING);

                renderers = new HideableBarRenderer[1];
                renderers[0] = new HideableBarRenderer(new NumberPercentRenderer(Formatters.bytesFormat()));

                LabelRenderer threadRenderer = new LabelRenderer();
                threadRenderer.setIcon(Icons.getIcon(ProfilerIcons.THREAD));
                threadRenderer.setFont(threadRenderer.getFont().deriveFont(Font.BOLD));

                table.setColumnRenderer(0, threadRenderer);
                table.setColumnRenderer(1, renderers[0]);

                add(new ProfilerTableContainer(table, false, null), BorderLayout.CENTER);
            }
        }
        
        
        private class TreadsAllocTableModel extends AbstractTableModel {
        
            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) {
                    return "Thread";
                } else if (columnIndex == 1) {
                    return "Allocated";
                }

                return null;
            }

            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return String.class;
                } else {
                    return Long.class;
                }
            }

            public int getRowCount() {
                return names == null ? 0 : names.length;
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return names[rowIndex];
                } else if (columnIndex == 1) {
                    return values[rowIndex];
                }

                return null;
            }

        }
        
    }
    
}
