import { createStore } from 'polotno/model/store';
import { PolotnoContainer, SidePanelWrap, WorkspaceWrap } from 'polotno';
import { Workspace } from 'polotno/canvas/workspace';
import { Toolbar } from 'polotno/toolbar/toolbar';
import { ZoomButtons } from 'polotno/toolbar/zoom-buttons';
import { PagesTimeline } from 'polotno/pages-timeline';
import { SidePanel } from 'polotno/side-panel';
import { connectNativeBridge } from './nativeBridge';

const store = createStore({
  // Create your own key at https://polotno.com/cabinet
  key: import.meta.env.VITE_POLOTNO_KEY || 'YOUR_API_KEY',
  showCredit: true,
});

const inApp = !!window.PolotnoAndroid;
if (inApp) {
  // Save and export buttons live in the native app bar.
  connectNativeBridge(store);
} else {
  // Plain browser (npm run dev): start with an empty page.
  store.addPage();
}

export default function App() {
  return (
    <PolotnoContainer style={{ width: '100%', height: '100%' }}>
      <SidePanelWrap>
        <SidePanel store={store} />
      </SidePanelWrap>
      <WorkspaceWrap>
        {/* A browser download does nothing in a WebView; keep it for npm run dev. */}
        <Toolbar store={store} downloadButtonEnabled={!inApp} />
        <Workspace store={store} />
        <ZoomButtons store={store} />
        <PagesTimeline store={store} />
      </WorkspaceWrap>
    </PolotnoContainer>
  );
}
