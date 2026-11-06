import { ANDROID_RELEASE } from "@/content/download";
import { Reveal } from "../Reveal";

export function Download() {
  return (
    <section id="download" className="shell section border-t border-line" aria-labelledby="download-heading">
      <Reveal>
        <span className="eyebrow">Android early release</span>
        <h2 id="download-heading" className="h2 mt-6">Start with your everyday money.</h2>
        <p className="mt-6 max-w-2xl text-ink-2">Download Kosh, the Android app behind Tinward. This early release tracks supported transaction messages and statement imports. The website shows our broader vision; some features and the appearance shown here are still in development.</p>
        <div className="mt-8 flex flex-wrap items-center gap-6">
          <a className="btn btn-primary" href={ANDROID_RELEASE.apkUrl}>Download Android APK <span aria-hidden="true">↓</span></a>
          <a href={ANDROID_RELEASE.releaseUrl} className="underline underline-offset-4">Release notes &amp; checksum</a>
        </div>
        <p className="mt-4 text-sm text-ink-2">Version {ANDROID_RELEASE.version} · Android {ANDROID_RELEASE.minimumAndroid}+ · Free · Early release</p>
        <ol className="mt-8 max-w-2xl list-decimal space-y-3 pl-5 text-ink-2">
          <li>Download the APK on your Android phone and open it. Allow installation from your browser or file manager if Android asks.</li>
          <li>Open Kosh. Allow SMS access for supported message tracking, or import a supported bank statement.</li>
          <li>Review uncertain transactions and compare totals with your bank records. Coverage and categorisation are still being improved.</li>
        </ol>
        <p className="mt-6 max-w-2xl rounded-xl border border-line p-5 text-sm text-ink-2"><strong>Already using Kosh?</strong> This release uses a new signing key and cannot update an installation signed with the old key. If Android rejects the update, keep your existing app installed. Uninstalling removes its local data. This download does not migrate data from another installation.</p>
      </Reveal>
    </section>
  );
}
