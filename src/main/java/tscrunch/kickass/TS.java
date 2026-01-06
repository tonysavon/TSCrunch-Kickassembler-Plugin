package tscrunch.kickass;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import kickass.plugins.interf.general.IEngine;
import kickass.plugins.interf.general.IMemoryBlock;
import kickass.plugins.interf.general.IParameterMap;
import kickass.plugins.interf.general.IValue;
import tscrunch.kickass.core.AbstractCruncher;
import tscrunch.kickass.core.CrunchedObject;
import tscrunch.kickass.core.Options;
import tscrunch.kickass.core.Utils;
import tscrunch.TSCrunch;

public class TS extends AbstractCruncher {

    private static final String NAME = "TS";
    private boolean lastInplace;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getSyntax() {
        return NAME + " (boolean inplace [false], boolean sfx [false], int jmpAddress [start])";
    }

    @Override
    protected CrunchedObject crunch(IMemoryBlock block, EnumMap<Options, Object> opts, IEngine engine) {
        boolean inplace = opts.containsKey(Options.INPLACE);
        boolean sfx = opts.containsKey(Options.SFX);
        int jmpAddress = (Integer) opts.get(Options.JMP_ADDRESS);
        lastInplace = inplace;

        byte[] data = TSCrunch.crunchForPlugin(block.getBytes(), block.getStartAddress(), inplace, sfx, jmpAddress);
        if (data == null) {
            engine.error(NAME + " failed to crunch data");
            return new CrunchedObject(new byte[0], block.getStartAddress());
        }
        int displayAddress = block.getStartAddress();
        if (sfx) {
            displayAddress = 0x0801;
        }
        if (inplace) {
            int decrunchEnd = (block.getStartAddress() + block.getBytes().length - 1) & 0xffff;
            displayAddress = (decrunchEnd - data.length + 1) & 0xffff;
        }
        return new CrunchedObject(data, displayAddress);
    }

    @Override
    protected byte[] finalizeData(List<IMemoryBlock> blocks, EnumMap<Options, Object> options,
                                  IEngine engine, List<CrunchedObject> objects) {
        return objects.get(0).data;
    }

    @Override
    protected String formatAddress(int address) {
        if (lastInplace) {
            return "Load address: " + Utils.toHexString(address);
        }
        return "Decrunch to: " + Utils.toHexString(address);
    }

    @Override
    protected void validateArguments(EnumMap<Options, Object> opts, List<IMemoryBlock> blocks,
                                     IValue[] values, IEngine engine) {
        if (blocks.isEmpty()) {
            return;
        }
        if (blocks.size() > 1) {
            engine.error(NAME + " only handles one, single memory block");
        }
        int defaultJmp = blocks.get(0).getStartAddress();
        addBooleanOption(values, 0, opts, Options.INPLACE, false);
        addBooleanOption(values, 1, opts, Options.SFX, false);
        addIntegerOption(values, 2, opts, Options.JMP_ADDRESS, defaultJmp);

        if (opts.containsKey(Options.SFX) && opts.containsKey(Options.INPLACE)) {
            engine.error(NAME + " cannot use sfx and inplace at the same time");
        }
    }

    @Override
    protected void validateArguments(EnumMap<Options, Object> opts, List<IMemoryBlock> blocks,
                                     IParameterMap params, IEngine engine) {
        if (blocks.isEmpty()) {
            return;
        }
        if (blocks.size() > 1) {
            engine.error(NAME + " only handles one, single memory block");
        }
        int defaultJmp = blocks.get(0).getStartAddress();
        addBooleanOption(params, opts, Options.INPLACE, false);
        addBooleanOption(params, opts, Options.SFX, false);
        addIntegerOption(params, opts, Options.JMP_ADDRESS, defaultJmp);

        if (opts.containsKey(Options.SFX) && opts.containsKey(Options.INPLACE)) {
            engine.error(NAME + " cannot use sfx and inplace at the same time");
        }
    }

    @Override
    protected Set<String> getParams() {
        return Set.of(
            Options.INPLACE.getName(),
            Options.SFX.getName(),
            Options.JMP_ADDRESS.getName());
    }

}
