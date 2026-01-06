package tscrunch.kickass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import kickass.plugins.interf.IPlugin;
import kickass.plugins.interf.archive.IArchive;

import tscrunch.kickass.TS;
import tscrunch.kickass.core.AbstractCruncher;
import tscrunch.kickass.core.AbstractSegmentCruncher;

public class CruncherPlugins implements IArchive {

    @Override
    public List<IPlugin> getPluginObjects() {
        List<IPlugin> list = new ArrayList<>();
        list.add(new TS());

        list.addAll(
            list.stream()
                .filter(AbstractCruncher.class::isInstance)
                .map(AbstractCruncher.class::cast)
                .map(AbstractSegmentCruncher::new)
                .collect(Collectors.toList()));

        return list;
    }

}
