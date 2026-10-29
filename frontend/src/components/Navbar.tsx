import React from 'react';
import { Layers, Sparkles, Download, HelpCircle } from 'lucide-react';
import { ApiStatusDto } from '../types';

interface NavbarProps {
  apiStatus: ApiStatusDto | null;
  onLoadDemo: () => void;
  isLoadingDemo: boolean;
  onOpenHelp: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  apiStatus,
  onLoadDemo,
  isLoadingDemo,
  onOpenHelp,
}) => {
  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="h-9 w-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white shadow-sm">
            <Layers className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="font-bold text-lg text-slate-900 tracking-tight">Reconcilia AI</span>
              <span className="text-xs px-2 py-0.5 font-medium rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                Audit v1.0
              </span>
            </div>
            <p className="text-xs text-slate-500 hidden sm:block">Conciliação Financeira & Auditoria de Vendas</p>
          </div>
        </div>

        {/* Right Actions */}
        <div className="flex items-center space-x-3">
          {/* Backend Status Pill */}
          <div className="hidden md:flex items-center space-x-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-slate-50 border border-slate-200 text-slate-600">
            <span className={`h-2 w-2 rounded-full ${apiStatus ? 'bg-emerald-500 animate-pulse' : 'bg-amber-400'}`} />
            <span>{apiStatus ? 'Backend Online' : 'Conectando...'}</span>
            {apiStatus?.aiAvailable && (
              <span className="ml-1 text-indigo-600 font-semibold flex items-center">
                <Sparkles className="h-3 w-3 mr-0.5" /> LLM
              </span>
            )}
          </div>

          {/* Sample CSV Downloads */}
          <div className="hidden lg:flex items-center space-x-1">
            <a
              href="/api/reconciliation/demo/erp-sample.csv"
              download
              className="inline-flex items-center px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-md transition"
              title="Baixar CSV exemplo de Vendas ERP"
            >
              <Download className="h-3.5 w-3.5 mr-1 text-slate-400" />
              CSV ERP
            </a>
            <a
              href="/api/reconciliation/demo/acquirer-sample.csv"
              download
              className="inline-flex items-center px-2.5 py-1.5 text-xs font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-md transition"
              title="Baixar CSV exemplo de Extrato Adquirente"
            >
              <Download className="h-3.5 w-3.5 mr-1 text-slate-400" />
              CSV Adquirente
            </a>
          </div>

          {/* Help Button */}
          <button
            onClick={onOpenHelp}
            className="p-1.5 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-md transition"
            title="Como usar e formato dos arquivos"
          >
            <HelpCircle className="h-4 w-4" />
          </button>

          {/* Demo Button */}
          <button
            onClick={onLoadDemo}
            disabled={isLoadingDemo}
            className="inline-flex items-center px-3.5 py-1.5 text-xs font-medium text-indigo-700 bg-indigo-50 border border-indigo-200 hover:bg-indigo-100 hover:border-indigo-300 rounded-md shadow-sm transition disabled:opacity-50"
          >
            <Sparkles className="h-3.5 w-3.5 mr-1.5 text-indigo-600" />
            {isLoadingDemo ? 'Carregando Demo...' : 'Carregar Demo (1-Clique)'}
          </button>
        </div>
      </div>
    </header>
  );
};
