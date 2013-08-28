package au.com.gaiaresources.bdrs.dwca;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.gbif.dwc.record.DarwinCoreRecord;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.text.Archive;
import org.gbif.dwc.text.ArchiveFactory;
import org.gbif.dwc.text.StarRecord;
import org.junit.Test;

import au.com.gaiaresources.bdrs.model.record.Record;
import au.com.gaiaresources.bdrs.model.record.impl.ScrollableRecordsList;
import au.com.gaiaresources.bdrs.util.FileUtils;
import au.com.gaiaresources.bdrs.util.ZipUtils;

public class RecordDwcaWriterTest extends AbstractDwcaTest {

    @SuppressWarnings("deprecation")
    @Test
    public void runtest() throws Exception {
        RecordDwcaWriter writer = new RecordDwcaWriter(lsidService, locService, rdService);
        
        List<Record> recordsToArchive = new ArrayList<Record>();
        recordsToArchive.add(r1);
        recordsToArchive.add(r2);
        recordsToArchive.add(r4);
        
        ScrollableRecordsList scrollableRecords = new ScrollableRecordsList(recordsToArchive);
        
        File targetDir = FileUtils.createTempDirectory("decompress_rec_dwca");
        File compressedFile = writer.writeArchive(scrollableRecords);
        ZipUtils.decompressToDir(compressedFile, targetDir);
        
        // check for file existance
        File metaFile = new File(targetDir.getAbsolutePath() + "/" + RecordDwcaWriter.META_FILE);
        File occurrenceFile = new File(targetDir.getAbsolutePath() + "/" + RecordDwcaWriter.OCCURRENCE_FILE);
        File mofFile = new File(targetDir.getAbsolutePath() + "/" + RecordDwcaWriter.MEASUREMENT_OR_FACT_FILE);
        
        Assert.assertTrue("meta file should exist", metaFile.exists());
        Assert.assertTrue("occurrence file should exist", occurrenceFile.exists());
        Assert.assertTrue("measurement/fact file should exist", mofFile.exists());

        Archive archive = ArchiveFactory.openArchive(targetDir);

        Assert.assertTrue("should have sci name field", archive.getCore().hasTerm(DwcTerm.scientificName));
        
        Iterator<DarwinCoreRecord> dwcIter = archive.iteratorDwc();

        int count = 0;
        while (dwcIter.hasNext()) {
            DarwinCoreRecord dwcr = dwcIter.next();
            StarRecord starRecord = getStarRecord(archive, dwcr.getCatalogNumber());
            
            this.assertDwcRecord(dwcr, starRecord, allRecordList);
            ++count;
        }
        
        Assert.assertEquals("incorrect record count", recordsToArchive.size(), count);
    }
}
