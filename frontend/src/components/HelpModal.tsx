import React from 'react';
import { X, ShieldCheck, FileText, Download } from 'lucide-react';

interface HelpModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const HelpModal: React.FC<HelpModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-2xl w-full border border-slate-200 shadow-xl overflow-hidden transition-all">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50">
          <div className="flex items-center space-x-2">
            <FileText className="h-5 w-5 text-indigo-600" />
            <h3 className="font-bold text-slate-900 text-sm">Guia de Uso & Formato dos Arquivos</h3>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-5 text-xs text-slate-600 leading-relaxed max-h-[75vh] overflow-y-auto">
          {/* Section 1: Overview */}
          <div>
            <h4 className="font-semibold text-slate-900 text-sm mb-1">Como funciona o Reconcilia AI?</h4>
            <p>
              O sistema cruza registros de vendas exportados de um ERP (como Bling, Tiny, Omie ou SAP) com os extratos de liquidação de adquirentes e bancos (Cielo, Stone, PagBank, Rede, etc.) de forma 100% determinística.
            </p>
          </div>

          {/* Section 2: Expected Format */}
          <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 space-y-2">
            <h4 className="font-semibold text-slate-900">Formato CSV Esperado</h4>
            <ul className="list-disc pl-5 space-y-1">
              <li><strong>Separadores aceitos:</strong> Ponto e vírgula (<code>;</code>), vírgula (<code>,</code>) ou tabulação (<code>\t</code>).</li>
              <li><strong>Formato monetário:</strong> Padrão brasileiro (<code>R$ 1.250,50</code> ou <code>1250,50</code>) ou internacional (<code>1250.50</code>).</li>
              <li><strong>Formatos de data:</strong> <code>DD/MM/AAAA</code> ou <code>AAAA-MM-DD</code>.</li>
              <li><strong>Colunas necessárias:</strong> Identificador (ID/NSU), Data e Valor. Descrição é opcional.</li>
            </ul>
          </div>

          {/* Section 3: Statuses */}
          <div>
            <h4 className="font-semibold text-slate-900 mb-2">Classificação das Transações</h4>
            <div className="grid grid-cols-2 gap-2 text-[11px]">
              <div className="p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800">
                <strong>Conciliado (MATCHED):</strong> Valores e datas coincidem dentro das tolerâncias estipuladas.
              </div>
              <div className="p-2.5 rounded-lg bg-amber-50 border border-amber-200 text-amber-800">
                <strong>Divergência de Valor:</strong> Mesmo ID ou venda correlacionada, com diferença de taxa MDR, gorjeta ou desconto.
              </div>
              <div className="p-2.5 rounded-lg bg-sky-50 border border-sky-200 text-sky-800">
                <strong>Divergência de Data:</strong> Mesmo valor/venda, porém com defasagem de repasse (D+1, feriados).
              </div>
              <div className="p-2.5 rounded-lg bg-rose-50 border border-rose-200 text-rose-800">
                <strong>Não Conciliado:</strong> Presente em apenas uma das fontes (pedido cancelado ou venda direta em POS).
              </div>
            </div>
          </div>

          {/* Section 4: Privacy & AI */}
          <div className="p-3.5 rounded-xl bg-indigo-50/60 border border-indigo-100 flex items-start space-x-2.5">
            <ShieldCheck className="h-5 w-5 text-indigo-600 mt-0.5 shrink-0" />
            <div>
              <strong className="text-indigo-900 block font-semibold mb-0.5">Segurança & IA Responsável</strong>
              <p className="text-slate-600 text-[11px]">
                Nenhum arquivo ou dado financeiro é gravado em banco de dados ou persistido em disco. O processamento ocorre exclusivamente em memória. As hipóteses formuladas por IA utilizam apenas metadados da discrepância e servem como apoio investigativo.
              </p>
            </div>
          </div>

          {/* Download sample buttons */}
          <div className="pt-2 flex items-center justify-between border-t border-slate-100">
            <span className="text-xs text-slate-500 font-medium">Arquivos sintéticos para teste:</span>
            <div className="flex space-x-2">
              <a
                href="/api/reconciliation/demo/erp-sample.csv"
                download
                className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-50 rounded-lg shadow-xs"
              >
                <Download className="h-3.5 w-3.5 mr-1" />
                Exemplo ERP (.csv)
              </a>
              <a
                href="/api/reconciliation/demo/acquirer-sample.csv"
                download
                className="inline-flex items-center px-3 py-1.5 text-xs font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-50 rounded-lg shadow-xs"
              >
                <Download className="h-3.5 w-3.5 mr-1" />
                Exemplo Extrato (.csv)
              </a>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-1.5 text-xs font-semibold text-white bg-slate-900 hover:bg-slate-800 rounded-lg transition"
          >
            Entendido
          </button>
        </div>
      </div>
    </div>
  );
};
