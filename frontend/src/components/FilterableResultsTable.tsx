import React, { useState, useMemo } from 'react';
import { Search, Download, Eye } from 'lucide-react';
import { ReconciliationItemDto, ReconciliationStatus } from '../types';
import { formatBRL, formatDateBR, getStatusBadgeConfig } from '../utils/formatters';

interface FilterableResultsTableProps {
  items: ReconciliationItemDto[];
  onSelectItem: (item: ReconciliationItemDto) => void;
  onExportCsv: () => void;
  isExporting: boolean;
}

export const FilterableResultsTable: React.FC<FilterableResultsTableProps> = ({
  items,
  onSelectItem,
  onExportCsv,
  isExporting,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | ReconciliationStatus>('ALL');
  const [sourceFilter, setSourceFilter] = useState<'ALL' | 'ERP_ONLY' | 'ACQUIRER_ONLY'>('ALL');

  const counts = useMemo(() => {
    return {
      all: items.length,
      matched: items.filter((i) => i.status === 'MATCHED').length,
      amountMismatch: items.filter((i) => i.status === 'AMOUNT_MISMATCH').length,
      dateMismatch: items.filter((i) => i.status === 'DATE_MISMATCH').length,
      unmatched: items.filter((i) => i.status === 'UNMATCHED').length,
    };
  }, [items]);

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      // Status filter
      if (statusFilter !== 'ALL' && item.status !== statusFilter) {
        return false;
      }

      // Source filter
      if (sourceFilter === 'ERP_ONLY' && (!item.sourceA || item.sourceB)) {
        return false;
      }
      if (sourceFilter === 'ACQUIRER_ONLY' && (!item.sourceB || item.sourceA)) {
        return false;
      }

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const idA = item.sourceA?.id.toLowerCase() || '';
        const idB = item.sourceB?.id.toLowerCase() || '';
        const descA = item.sourceA?.description.toLowerCase() || '';
        const descB = item.sourceB?.description.toLowerCase() || '';
        const hyp = item.aiHypothesis?.toLowerCase() || '';
        const amtA = item.sourceA?.amount.toString() || '';
        const amtB = item.sourceB?.amount.toString() || '';

        return (
          idA.includes(q) ||
          idB.includes(q) ||
          descA.includes(q) ||
          descB.includes(q) ||
          hyp.includes(q) ||
          amtA.includes(q) ||
          amtB.includes(q)
        );
      }

      return true;
    });
  }, [items, statusFilter, sourceFilter, searchQuery]);

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      {/* Table Toolbar */}
      <div className="p-4 border-b border-slate-200 flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white">
        {/* Status Filter Tabs */}
        <div className="flex items-center space-x-1 overflow-x-auto pb-1 md:pb-0 text-xs">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`px-3 py-1.5 rounded-lg font-medium transition whitespace-nowrap ${
              statusFilter === 'ALL'
                ? 'bg-slate-900 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            Todas ({counts.all})
          </button>
          <button
            onClick={() => setStatusFilter('MATCHED')}
            className={`px-3 py-1.5 rounded-lg font-medium transition whitespace-nowrap ${
              statusFilter === 'MATCHED'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            Conciliadas ({counts.matched})
          </button>
          <button
            onClick={() => setStatusFilter('AMOUNT_MISMATCH')}
            className={`px-3 py-1.5 rounded-lg font-medium transition whitespace-nowrap ${
              statusFilter === 'AMOUNT_MISMATCH'
                ? 'bg-amber-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            Dif. Valor ({counts.amountMismatch})
          </button>
          <button
            onClick={() => setStatusFilter('DATE_MISMATCH')}
            className={`px-3 py-1.5 rounded-lg font-medium transition whitespace-nowrap ${
              statusFilter === 'DATE_MISMATCH'
                ? 'bg-sky-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            Dif. Data ({counts.dateMismatch})
          </button>
          <button
            onClick={() => setStatusFilter('UNMATCHED')}
            className={`px-3 py-1.5 rounded-lg font-medium transition whitespace-nowrap ${
              statusFilter === 'UNMATCHED'
                ? 'bg-rose-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            Não Conciliadas ({counts.unmatched})
          </button>
        </div>

        {/* Search & Export Actions */}
        <div className="flex items-center space-x-2">
          {/* Source Filter */}
          <select
            value={sourceFilter}
            onChange={(e) => setSourceFilter(e.target.value as any)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2 py-1.5 text-slate-700 focus:outline-hidden cursor-pointer"
          >
            <option value="ALL">Todas as Fontes</option>
            <option value="ERP_ONLY">Somente ERP (Ausente Adquirente)</option>
            <option value="ACQUIRER_ONLY">Somente Adquirente (Ausente ERP)</option>
          </select>

          {/* Search Box */}
          <div className="relative">
            <Search className="h-4 w-4 absolute left-2.5 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="Buscar por ID, descrição..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg text-slate-800 placeholder-slate-400 focus:outline-hidden focus:ring-1 focus:ring-indigo-500 w-48 sm:w-64"
            />
          </div>

          {/* Export CSV Button */}
          <button
            onClick={onExportCsv}
            disabled={isExporting || items.length === 0}
            className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-50 rounded-lg shadow-xs transition disabled:opacity-50"
          >
            <Download className="h-3.5 w-3.5 mr-1.5 text-slate-500" />
            {isExporting ? 'Exportando...' : 'Exportar CSV'}
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-left text-xs">
          <thead className="bg-slate-50 text-slate-600 font-semibold tracking-wider uppercase text-[11px]">
            <tr>
              <th scope="col" className="px-4 py-3">Status</th>
              <th scope="col" className="px-4 py-3">Fonte A (ERP)</th>
              <th scope="col" className="px-4 py-3">Fonte B (Adquirente)</th>
              <th scope="col" className="px-4 py-3">Divergência</th>
              <th scope="col" className="px-4 py-3">Hipótese / Diagnóstico</th>
              <th scope="col" className="px-4 py-3 text-right">Ação</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {filteredItems.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-10 text-center text-slate-400 text-xs">
                  Nenhuma transação encontrada com os filtros selecionados.
                </td>
              </tr>
            ) : (
              filteredItems.map((item) => {
                const badge = getStatusBadgeConfig(item.status);
                const a = item.sourceA;
                const b = item.sourceB;

                return (
                  <tr key={item.id} className="hover:bg-slate-50/70 transition">
                    {/* Status */}
                    <td className="px-4 py-3 whitespace-nowrap">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full font-medium border text-[11px] ${badge.bg}`}>
                        <span className={`h-1.5 w-1.5 rounded-full mr-1.5 ${badge.dot}`} />
                        {badge.label}
                      </span>
                    </td>

                    {/* ERP */}
                    <td className="px-4 py-3">
                      {a ? (
                        <div>
                          <div className="flex items-center space-x-2">
                            <span className="font-semibold text-slate-900 font-mono">{a.id || '(sem id)'}</span>
                            <span className="text-[11px] text-slate-400 font-medium">{formatDateBR(a.date)}</span>
                          </div>
                          <div className="text-slate-800 font-bold">{formatBRL(a.amount)}</div>
                          <div className="text-[11px] text-slate-500 truncate max-w-xs">{a.description || '-'}</div>
                        </div>
                      ) : (
                        <span className="text-slate-400 italic text-[11px]">Ausente no ERP</span>
                      )}
                    </td>

                    {/* Acquirer */}
                    <td className="px-4 py-3">
                      {b ? (
                        <div>
                          <div className="flex items-center space-x-2">
                            <span className="font-semibold text-slate-900 font-mono">{b.id || '(sem id)'}</span>
                            <span className="text-[11px] text-slate-400 font-medium">{formatDateBR(b.date)}</span>
                          </div>
                          <div className="text-slate-800 font-bold">{formatBRL(b.amount)}</div>
                          <div className="text-[11px] text-slate-500 truncate max-w-xs">{b.description || '-'}</div>
                        </div>
                      ) : (
                        <span className="text-slate-400 italic text-[11px]">Ausente no extrato</span>
                      )}
                    </td>

                    {/* Variance */}
                    <td className="px-4 py-3 whitespace-nowrap">
                      {item.amountDifference !== null && (
                        <div className={`font-semibold ${item.amountDifference < 0 ? 'text-amber-700' : 'text-slate-800'}`}>
                          {item.amountDifference === 0 ? 'R$ 0,00' : formatBRL(item.amountDifference)}
                        </div>
                      )}
                      {item.dateDifferenceDays !== null && item.dateDifferenceDays !== 0 && (
                        <div className="text-[11px] text-slate-500 font-medium">
                          {Math.abs(item.dateDifferenceDays)} dia(s) dif.
                        </div>
                      )}
                    </td>

                    {/* Hypothesis / Note */}
                    <td className="px-4 py-3 max-w-sm">
                      <p className="text-[11px] text-slate-600 line-clamp-2 leading-relaxed">
                        {item.aiHypothesis || item.matchReason}
                      </p>
                    </td>

                    {/* Action */}
                    <td className="px-4 py-3 text-right whitespace-nowrap">
                      <button
                        onClick={() => onSelectItem(item)}
                        className="inline-flex items-center px-2.5 py-1 text-xs font-medium text-indigo-600 hover:text-indigo-800 hover:bg-indigo-50 rounded-md transition"
                      >
                        <Eye className="h-3.5 w-3.5 mr-1" />
                        Detalhes
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Footer info */}
      <div className="p-3 bg-slate-50 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
        <span>Mostrando {filteredItems.length} de {items.length} registros</span>
        <span className="text-[11px]">Algoritmo determinístico com reconciliação 1-para-1 estrita</span>
      </div>
    </div>
  );
};
