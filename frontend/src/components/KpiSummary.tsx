import React from 'react';
import { CheckCircle2, AlertTriangle, Clock, HelpCircle, TrendingUp, TrendingDown, Layers } from 'lucide-react';
import { ReconciliationSummary } from '../types';
import { formatBRL } from '../utils/formatters';

interface KpiSummaryProps {
  summary: ReconciliationSummary;
}

export const KpiSummary: React.FC<KpiSummaryProps> = ({ summary }) => {
  const isNetDiscrepancyNegative = summary.netDiscrepancyAmount < 0;

  return (
    <div className="space-y-4 mb-8">
      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Volume ERP */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Volume Fonte A (ERP)</span>
            <span className="text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-600 font-semibold">
              {summary.totalSourceACount} vendas
            </span>
          </div>
          <p className="mt-2 text-2xl font-bold text-slate-900 tracking-tight">
            {formatBRL(summary.totalSourceAAmount)}
          </p>
          <div className="mt-3 flex items-center text-xs text-slate-500">
            <span>Conciliado: {formatBRL(summary.matchedAmountSourceA)}</span>
          </div>
        </div>

        {/* Card 2: Volume Adquirente */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Volume Fonte B (Extrato)</span>
            <span className="text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-600 font-semibold">
              {summary.totalSourceBCount} lançamentos
            </span>
          </div>
          <p className="mt-2 text-2xl font-bold text-slate-900 tracking-tight">
            {formatBRL(summary.totalSourceBAmount)}
          </p>
          <div className="mt-3 flex items-center text-xs text-slate-500">
            <span>Conciliado: {formatBRL(summary.matchedAmountSourceB)}</span>
          </div>
        </div>

        {/* Card 3: Taxa de Conciliação */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Taxa de Conciliação</span>
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
          </div>
          <p className="mt-2 text-2xl font-bold text-slate-900 tracking-tight">
            {summary.matchRatePercentage.toFixed(1)}%
          </p>
          {/* Progress bar */}
          <div className="mt-3 w-full bg-slate-100 rounded-full h-1.5 overflow-hidden">
            <div
              className="bg-emerald-500 h-1.5 rounded-full transition-all duration-500"
              style={{ width: `${Math.min(100, Math.max(0, summary.matchRatePercentage))}%` }}
            />
          </div>
        </div>

        {/* Card 4: Divergência Líquida */}
        <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">Divergência Líquida</span>
            {isNetDiscrepancyNegative ? (
              <TrendingDown className="h-4 w-4 text-amber-600" />
            ) : (
              <TrendingUp className="h-4 w-4 text-emerald-600" />
            )}
          </div>
          <p className={`mt-2 text-2xl font-bold tracking-tight ${isNetDiscrepancyNegative ? 'text-amber-700' : 'text-slate-900'}`}>
            {formatBRL(summary.netDiscrepancyAmount)}
          </p>
          <div className="mt-3 flex items-center text-xs text-slate-500">
            <span>Diferença entre Extrato e ERP</span>
          </div>
        </div>
      </div>

      {/* Discrepancy Breakdown Strip */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-xs flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center space-x-2 text-slate-700 font-medium">
          <Layers className="h-4 w-4 text-indigo-600" />
          <span>Status das Transações:</span>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200 font-medium">
            <CheckCircle2 className="h-3.5 w-3.5 mr-1 text-emerald-600" />
            {summary.matchedCount} Conciliadas
          </span>

          <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-amber-50 text-amber-700 border border-amber-200 font-medium">
            <AlertTriangle className="h-3.5 w-3.5 mr-1 text-amber-600" />
            {summary.amountMismatchCount} Divergências de Valor
          </span>

          <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-sky-50 text-sky-700 border border-sky-200 font-medium">
            <Clock className="h-3.5 w-3.5 mr-1 text-sky-600" />
            {summary.dateMismatchCount} Divergências de Data
          </span>

          <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-rose-50 text-rose-700 border border-rose-200 font-medium">
            <HelpCircle className="h-3.5 w-3.5 mr-1 text-rose-600" />
            {summary.unmatchedSourceACount + summary.unmatchedSourceBCount} Não Conciliadas
          </span>
        </div>
      </div>
    </div>
  );
};
