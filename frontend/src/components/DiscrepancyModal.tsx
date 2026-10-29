import React from 'react';
import { X, Sparkles, Info } from 'lucide-react';
import { ReconciliationItemDto } from '../types';
import { formatBRL, formatDateBR, getStatusBadgeConfig } from '../utils/formatters';

interface DiscrepancyModalProps {
  item: ReconciliationItemDto | null;
  onClose: () => void;
}

export const DiscrepancyModal: React.FC<DiscrepancyModalProps> = ({ item, onClose }) => {
  if (!item) return null;

  const badgeConfig = getStatusBadgeConfig(item.status);
  const a = item.sourceA;
  const b = item.sourceB;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full border border-slate-200 shadow-xl overflow-hidden transition-all animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center space-x-3">
            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${badgeConfig.bg}`}>
              <span className={`h-1.5 w-1.5 rounded-full mr-1.5 ${badgeConfig.dot}`} />
              {badgeConfig.label}
            </span>
            <span className="text-xs text-slate-400">ID Item: {item.id.slice(0, 8)}</span>
          </div>

          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {/* Side-by-side comparison */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Fonte A (ERP) */}
            <div className={`p-4 rounded-xl border ${a ? 'bg-white border-slate-200' : 'bg-slate-50/60 border-dashed border-slate-200'}`}>
              <div className="flex items-center justify-between pb-2 mb-3 border-b border-slate-100">
                <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">Fonte A (ERP)</span>
                {a ? (
                  <span className="text-[11px] text-slate-400">Linha {a.rowNumber}</span>
                ) : (
                  <span className="text-[11px] text-rose-500 font-medium">Ausente</span>
                )}
              </div>

              {a ? (
                <div className="space-y-2 text-xs">
                  <div>
                    <span className="text-slate-400 block text-[11px]">Identificador:</span>
                    <span className="font-semibold text-slate-800 font-mono">{a.id || '(sem id)'}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Data da Venda:</span>
                    <span className="font-medium text-slate-800">{formatDateBR(a.date)}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Valor Registrado:</span>
                    <span className="font-bold text-slate-900 text-sm">{formatBRL(a.amount)}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Descrição / Produto:</span>
                    <span className="text-slate-700">{a.description || '-'}</span>
                  </div>
                </div>
              ) : (
                <div className="py-6 text-center text-xs text-slate-400 italic">
                  Nenhum lançamento correspondente encontrado no arquivo do ERP.
                </div>
              )}
            </div>

            {/* Fonte B (Adquirente) */}
            <div className={`p-4 rounded-xl border ${b ? 'bg-white border-slate-200' : 'bg-slate-50/60 border-dashed border-slate-200'}`}>
              <div className="flex items-center justify-between pb-2 mb-3 border-b border-slate-100">
                <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">Fonte B (Adquirente)</span>
                {b ? (
                  <span className="text-[11px] text-slate-400">Linha {b.rowNumber}</span>
                ) : (
                  <span className="text-[11px] text-rose-500 font-medium">Ausente</span>
                )}
              </div>

              {b ? (
                <div className="space-y-2 text-xs">
                  <div>
                    <span className="text-slate-400 block text-[11px]">NSU / Identificador:</span>
                    <span className="font-semibold text-slate-800 font-mono">{b.id || '(sem id)'}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Data de Liquidação:</span>
                    <span className="font-medium text-slate-800">{formatDateBR(b.date)}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Valor Creditado:</span>
                    <span className="font-bold text-slate-900 text-sm">{formatBRL(b.amount)}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Descrição / Detalhe:</span>
                    <span className="text-slate-700">{b.description || '-'}</span>
                  </div>
                </div>
              ) : (
                <div className="py-6 text-center text-xs text-slate-400 italic">
                  Nenhum lançamento correspondente localizado no extrato da adquirente.
                </div>
              )}
            </div>
          </div>

          {/* Variance Breakdown Strip */}
          <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 text-xs flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center space-x-2">
              <span className="font-semibold text-slate-700">Divergência Aritmética:</span>
            </div>
            <div className="flex items-center space-x-4">
              <div>
                <span className="text-slate-400 mr-1.5">Diferença de Valor:</span>
                <span className={`font-bold ${item.amountDifference && item.amountDifference < 0 ? 'text-amber-700' : 'text-slate-800'}`}>
                  {item.amountDifference !== null ? formatBRL(item.amountDifference) : '-'}
                </span>
              </div>
              <div>
                <span className="text-slate-400 mr-1.5">Defasagem de Data:</span>
                <span className="font-bold text-slate-800">
                  {item.dateDifferenceDays !== null ? `${Math.abs(item.dateDifferenceDays)} dia(s)` : '-'}
                </span>
              </div>
            </div>
          </div>

          {/* AI / Rule-based Hypothesis Card */}
          <div className="p-4 rounded-xl bg-indigo-50/50 border border-indigo-100">
            <div className="flex items-center space-x-2 mb-2">
              <Sparkles className="h-4 w-4 text-indigo-600" />
              <h4 className="text-xs font-bold text-indigo-900 uppercase tracking-wide">
                {item.isHypothesisAi ? 'Hipótese Formulada por IA' : 'Hipótese Baseada em Regras Financeiras'}
              </h4>
            </div>
            <p className="text-xs text-slate-700 leading-relaxed font-medium">
              {item.aiHypothesis || 'Nenhuma hipótese gerada para este registro.'}
            </p>

            {/* Disclaimer */}
            <div className="mt-3 pt-2 border-t border-indigo-100/80 flex items-start space-x-1.5 text-[11px] text-indigo-600/90">
              <Info className="h-3.5 w-3.5 mt-0.5 shrink-0" />
              <span>
                <strong>Aviso de Auditoria:</strong> Esta análise é uma hipótese estatística/interpretativa baseada estritamente nos dados das transações e não substitui comprovação documental oficial.
              </span>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-100 rounded-lg transition"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};
