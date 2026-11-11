import fs from 'node:fs';
import {createRequire} from 'node:module';
const require=createRequire(new URL('../frontend/package.json',import.meta.url));
const React=require('react');const {renderToStaticMarkup}=require('react-dom/server');const icons=require('lucide-react');
const names=['PawPrint','LayoutDashboard','HeartPulse','Users','House','Utensils','Stethoscope','ShieldCheck','UserRound','CircleHelp','ArrowRight','ChevronRight','ChevronLeft','Plus','Search','Menu','X','NotebookPen','ShieldAlert','QrCode','Printer','Mail','Clock','Check','RefreshCw','LogOut','KeyRound','CalendarDays','Download','Inbox','CircleCheck','TriangleAlert','Camera','ClipboardList'];
fs.writeFileSync('figma-design/icon-sources.json',JSON.stringify(Object.fromEntries(names.map(name=>[name,renderToStaticMarkup(React.createElement(icons[name],{size:20,color:'#226a54',strokeWidth:1.8}))]))));
