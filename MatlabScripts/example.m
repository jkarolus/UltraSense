close all
clear
lightblue = [(0/255), (125/255), (178/255)];
darkgreen = [(0/255), (91/255), (11/255)];


%provide path to wav files (folder)
filepath = 'wave';
%filter those files you want
files = dir(strcat(filepath, '/*.wav'));

%enable/disable plots
plot_spectrogram = 1;               % complete spectrogram of each wave file
plot_spec_thresholded = 0;          % spectrogram (only around 20kHz range), filtered by fft_filter and thresholded
plot_surf = 0;                      % spectrogram as surf plot (only around 20kHz range), filtered by fft_filter and thresholded

%hull plots are done on the filtered FFT data and interpolated/filtered again
plot_hull = 1;                      % hull plot of the doppler broadening containing all files and their mean
plot_individual_hull = 0;           % hull plot for each wave file


%tresholding, smoothing paras
db_threshold = -65;                 % threshold signal for plots
fft_filter_sigma = 2.0;             % sigma for gaussian filter in FFT domain
fft_fsize = [3 10];                 % filter size in FFT domain

f_min = 1.95e4;                     % frequency band to analyze and show in surf, hull plots
f_max = 2.05e4;

hull_filter_sigma = 2.0;            % sigma and fsize of gaussian filter used on the hull plots
hull_fsize = [1 10];
hull_interp_factor = 4;             % factor of interpolation used on the hull plots


%FFT paras
wlen = 4096;                        % window length (recomended to be power of 2)
h = wlen/4;                         % hop size (recomended to be power of 2)
nfft = wlen;                        % number of fft points (recomended to be power of 2)



%get name for overall figure and the length of the signals
name = '';
lengths = zeros(1, length(files));
for i=1:length(files)
    name = strcat(strcat(name, ', '), files(i).name);
    [x, fs] = wavread(strcat(strcat(filepath, '/'), files(i).name));
    x = x(:,1);
    lengths(i) = length(x);
end
t_min = (wlen/2:h:min(lengths)-wlen/2-1)/fs;
high_doppler = zeros(length(files), length(t_min));
low_doppler = zeros(length(files), length(t_min));


%FFT and doppler analysis for each file
for i=1:length(files)
    % load a .wav file
    [x, fs] = wavread(strcat(strcat(filepath, '/'), files(i).name));
    x = x(:, 1);                        % get the first channel
    xmax = max(abs(x));                 % find the maximum abs value
    x = x/xmax;                         % scalling the signal

    % define analysis parameters
    xlen = length(x);                   % length of the signal

    % define the coherent amplification of the window
    K = sum(hann(wlen, 'periodic'))/wlen;
    
    % perform STFT
    [s, f, t] = stft(x, wlen, h, nfft, fs);

    % take the amplitude of fft(x) and scale it, so not to be a
    % function of the length of the window and its coherent amplification
    s = abs(s)/wlen/K;

    % correction of the DC & Nyquist component
    if rem(nfft, 2)                     % odd nfft excludes Nyquist point
        st(2:end, :) = s(2:end, :).*2;
    else                                % even nfft includes Nyquist point
        s(2:end-1, :) = s(2:end-1, :).*2;
    end

    % convert amplitude spectrum to dB (min = -120 dB)
    s = 20*log10(s + 1e-6);
    
    % plot the spectrogram
    if(plot_spectrogram)
        figure
        imagesc(t, f, s);
        set(gca,'YDir','normal')
        set(gca, 'FontName', 'Times New Roman', 'FontSize', 14)
        xlabel('Time, s')
        ylabel('Frequency, Hz')
        title(strcat('Amplitude spectrogram of the signal: ', strrep(files(i).name, '_', '\_')));
        handl = colorbar;
        set(handl, 'FontName', 'Times New Roman', 'FontSize', 14)
        ylabel(handl, 'Magnitude, dB')
    end


    
    
    %restrict frequency band
    f_indices = find(f > f_min & f < f_max);
    s2 = s(f_indices, :);
    
    %filter
    filter = fspecial('gaussian', fft_fsize, fft_filter_sigma);
    s2 = imfilter(s2, filter, 'replicate');
    
    %threshold
    s2(s2<db_threshold)=-90;
    
    if(plot_spec_thresholded)
        figure
        imagesc(t, f(f_indices), s2);
        set(gca,'YDir','normal')
        set(gca, 'FontName', 'Times New Roman', 'FontSize', 14)
        xlabel('Time, s')
        ylabel('Frequency, Hz')
        title(strcat('Amplitude spectrogram of the signal: ', strrep(files(i).name, '_', '\_')));
        handl = colorbar;
        set(handl, 'FontName', 'Times New Roman', 'FontSize', 14)
        ylabel(handl, 'Magnitude, dB')
    end
        
    
    %plot surf of the signal (for each file)
    if(plot_surf)
        figure
        surf(t, f(f_indices), s2);
        title(strcat('surf of: ', strrep(files(i).name, '_', '\_')));
        axis([t(1) t(end) f_min f_max -90 -10]);
    end

    %calculate doppler broadening, accumulate for all signals
    for j=1:length(t_min)
        currentRow = s2(:,j);
        [~, max_col] = max(currentRow);
        for k=max_col:size(s2,1)
            if(currentRow(k) > -90)
                high_doppler(i, j)= high_doppler(i, j)+1;
            else
                break;
            end
        end

        for k=max_col:-1:1
            if(currentRow(k) > -90)
                low_doppler(i, j)= low_doppler(i, j)-1;
            else
                break;
            end
        end
    end

    
    if(plot_individual_hull)
        %calculate doppler broadening for this specific signal
        high = zeros(1, length(t));
        low = zeros(1, length(t));
        for j=1:length(t)
            currentRow = s2(:,j);
            [~, max_col] = max(currentRow);
            for k=max_col:size(s2,1)
                if(currentRow(k) > -90)
                    high(j)= high(j)+1;
                else
                    break;
                end
            end

            for k=max_col:-1:1
                if(currentRow(k) > -90)
                    low(j)= low(j)-1;
                else
                    break;
                end
            end
        end
        
        figure;
        hold on;
        axis([min(t) max(t) -200 200]);
        title(strcat('Bandwidth extend given pivot: ', strrep(files(i).name, '_', '\_')));
        xlabel('Time, s');
        ylabel('Frequency, 1/s');

        high = high-min(high);
        low = low-max(low);
        
        high = high.*(44100/nfft);
        low = low.*(44100/nfft);

        high = interp(high, hull_interp_factor);
        low = interp(low, hull_interp_factor);

        filter2 = fspecial('gaussian', hull_fsize, hull_filter_sigma);
        high = imfilter(high, filter2, 'replicate');
        low = imfilter(low, filter2, 'replicate');

        plot(interp(t, hull_interp_factor), high, 'color', lightblue);
        plot(interp(t, hull_interp_factor), low, 'color', darkgreen);     
    end
end




%iterate again to plot the overall hull curve
if(plot_hull)
    figure;
    hold on;
    mean = zeros(2, length(t_min)*hull_interp_factor);
    axis([min(t_min) max(t_min) -200 200]);
    %set(gca,'ytick',[])
    title(strcat('Hull curve: ', strrep(name, '_', '\_')));
    xlabel('Time, s', 'FontSize', 11);
    ylabel('Frequency, Hz', 'FontSize', 11);

    for i=1:length(files)
        current_high = high_doppler(i,:);
        current_low = low_doppler(i,:);
        %current_high(current_high < mode(current_high)) = mode(current_high);
        %current_low(current_low > mode(current_low)) = mode(current_low);

        current_high = current_high-min(current_high);
        current_low = current_low-max(current_low);

        current_high = current_high.*(44100/nfft);
        current_low = current_low.*(44100/nfft);

        current_high = interp(current_high, hull_interp_factor);
        current_low = interp(current_low, hull_interp_factor);

        filter2 = fspecial('gaussian', hull_fsize, hull_filter_sigma);
        current_high = imfilter(current_high, filter2, 'replicate');
        current_low = imfilter(current_low, filter2, 'replicate');

        mean(1,:) = mean(1,:) + current_high;
        mean(2,:) = mean(2,:) + current_low;

        plot(interp(t_min, hull_interp_factor), current_high, 'color', lightblue);
        plot(interp(t_min, hull_interp_factor), current_low, 'color', darkgreen);
    end

    
    mean = mean./length(files);
    mean(1,:) = mean(1,:) - min(mean(1,:));   
    mean(2,:) = mean(2,:) - max(mean(2,:));

    %add mean
    plot(interp(t_min, hull_interp_factor), mean(1, :), 'color', lightblue, 'LineWidth', 3.0);
    plot(interp(t_min, hull_interp_factor), mean(2, :), 'color', darkgreen, 'LineWidth', 3.0);


end
