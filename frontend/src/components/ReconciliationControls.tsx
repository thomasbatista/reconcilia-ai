import React from 'react';
import { ArrowRight, RotateCcw, Sliders, Calendar, DollarSign } from 'lucide-react';

interface ReconciliationControlsProps {
  dateToleranceDays: number;
  amountTolerance: number;
  onDateToleranceChange: (days: number) => void;
  onAmountToleranceChange: (amount: number) => void;
  onReconcile: () => void;
  onReset: () => void;
  isProcessing: boolean;
  canReconcile: boolean;
}

export const ReconciliationControls: React.FC<ReconciliationControlsProps> = ({
  dateToleranceDays,
  amountTolerance,
  onDateToleranceChange,
  onAmountToleranceChange,
  onReconcile,
  onReset,
  isProcessing,
  canReconcile,
}) => {
  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 mb-8">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        {/* Tolerances Configuration */}
        <div className="flex flex-wrap items-center gap-6">
          <div className="flex items-center space-x-2 text-slate-700 font-medium text-xs">
            <Sliders className="h-4 w-4 text-indigo-600" />
            <span>Parâmetros de Tolerância:</span>
          </div>

          {/* Date Tolerance */}
          <div className="flex items-center space-x-2 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5">
            <Calendar className="h-4 w-4 text-slate-400" />
            <label className="text-xs text-slate-600 whitespace-nowrap">Janela de Data:</label>
            <select
              value={dateToleranceDays}
              onChange={(e) => onDateToleranceChange(parseInt(e.target.value, 10))}
              className="bg-transparent text-xs font-semibold text-slate-900 focus:outline-hidden cursor-pointer"
            >
              <option value={0}>Mesmo dia (0 dias)</option>
              <option value={1}>± 1 dia (D+1)</option>
              <option value={2}>± 2 dias (D+2)</option>
              <option value={3}>± 3 dias (Fim de semana)</option>
              <option value={5}>± 5 dias</option>
              <option value={7}>± 7 dias</option>
            </select>
          </div>

          {/* Amount Tolerance */}
          <div className="flex items-center space-x-2 bg-slate-50 border border-slate-200 rounded-lg px-3 py-1.5">
            <DollarSign className="h-4 w-4 text-slate-400" />
            <label className="text-xs text-slate-600 whitespace-nowrap">Tolerância de Valor:</label>
            <select
              value={amountTolerance}
              onChange={(e) => onAmountToleranceChange(parseFloat(e.target.value))}
              className="bg-transparent text-xs font-semibold text-slate-900 focus:outline-hidden cursor-pointer"
            >
              <option value={0.00}>Exato (R$ 0,00)</option>
              <option value={0.05}>Até R$ 0,05 (Centavos)</option>
              <option value={0.10}>Até R$ 0,10</option>
              <option value={1.00}>Até R$ 1,00</option>
              <option value={5.00}>Até R$ 5,00</option>
            </select>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center space-x-3">
          <button
            onClick={onReset}
            disabled={isProcessing}
            className="inline-flex items-center px-3 py-2 text-xs font-medium text-slate-600 hover:text-slate-800 bg-white border border-slate-200 hover:bg-slate-50 rounded-lg transition"
            title="Limpar arquivos e recomeçar"
          >
            <RotateCcw className="h-3.5 w-3.5 mr-1 text-slate-400" />
            Limpar
          </button>

          <button
            onClick={onReconcile}
            disabled={!canReconcile || isProcessing}
            className="inline-flex items-center justify-center px-5 py-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 disabled:cursor-not-allowed rounded-lg shadow-sm transition"
          >
            {isProcessing ? (
              <>
                <span className="animate-spin mr-2">◌</span>
                Processando conciliação...
              </>
            ) : (
              <>
                <span>Reconciliar Transações</span>
                <ArrowRight className="h-4 w-4 ml-1.5" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
